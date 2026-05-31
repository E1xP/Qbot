package com.bot.groupModeration.service;

import com.bot.CQGlobal;
import com.bot.entity.CQGroupUser;
import com.bot.event.message.CQGroupMessageEvent;
import com.bot.groupModeration.config.GroupModerationConfig;
import com.bot.groupModeration.detector.GantManOnnxNsfwDetector;
import com.bot.groupModeration.pojo.GroupModerationItem;
import com.bot.groupModeration.pojo.ModerationTask;
import com.bot.groupModeration.pojo.NsfwPrediction;
import com.bot.groupModeration.pojo.TriggerResult;
import com.bot.groupModeration.util.CqImageParser;
import com.bot.robot.CoolQ;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.File;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

/**
 * 群审核心服务：异步队列 + 单线程顺序推理 + 命中后处置。
 * <p>
 * 设计要点：
 * <ul>
 *   <li>插件线程 {@link #enqueue} 仅入队，不阻塞 CQ 消息回调</li>
 *   <li>后台 worker {@link #consumeLoop} 逐条 {@link #processTask}，避免 ONNX 并发占满 CPU</li>
 *   <li>一条消息可含多图，取 nsfw_ratio 最高且命中的结果作为整条消息的处置依据</li>
 *   <li>{@link #recentCache} 以图片内容 SHA-256 为键，仅缓存曾命中的推理结果（见 {@link #inspectImage}）</li>
 * </ul>
 */
@Service
@Slf4j
public class GroupModerationService {

    /**
     * 违规图识别结果缓存。key = 图片字节 SHA-256（hex），value = 分数与是否命中。
     * 仅 {@code triggered=true} 时写入；未命中图不缓存。进程内有效，无 TTL/容量上限。
     */
    private final Map<String, CachedHit> recentCache = new ConcurrentHashMap<>();
    @Resource
    private GroupModerationConfig config;
    @Resource
    private GantManOnnxNsfwDetector nsfwDetector;
    @Resource
    private ImageFetchService imageFetchService;
    @Resource
    private ImageStorageService imageStorageService;
    @Resource
    private ModerationActionService moderationActionService;
    private BlockingQueue<ModerationTask> queue;
    private ExecutorService worker;

    /**
     * 对图片原始字节做 SHA-256，用作 {@link #recentCache} 的 key（内容相同即视为同一张图）。
     */
    private static String sha256(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @PostConstruct
    public void startWorker() {
        int capacity = Math.max(16, config.getTaskQueueCapacity());
        queue = new LinkedBlockingQueue<>(capacity);
        worker = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "group-moderation-worker");
            t.setDaemon(true);
            return t;
        });
        worker.submit(this::consumeLoop);
        log.info("群审队列已启动 capacity={}", capacity);
    }

    @PreDestroy
    public void stopWorker() {
        if (worker != null) {
            worker.shutdownNow();
            try {
                worker.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static String segmentDisplayName(CqImageParser.CqImageSegment segment) {
        if (segment.getName() != null && !segment.getName().isEmpty()) {
            return segment.getName();
        }
        if (segment.getFile() != null && !segment.getFile().isEmpty()) {
            return segment.getFile();
        }
        if (segment.getUrl() != null && !segment.getUrl().isEmpty()) {
            return segment.getUrl();
        }
        return "-";
    }

    private void consumeLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                ModerationTask task = queue.take();
                processTask(task);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("群审消费异常", e);
            }
        }
    }

    /** 统一输出单张图的识别日志；{@code cache=true} 表示命中 {@link #recentCache}，未再跑 ONNX。 */
    private static void logRecognition(ModerationTask task, String fileName, String scores,
                                       float nsfwRatio, boolean triggered, boolean cache) {
        log.info("群审识别 groupId={} messageId={} userId={} nickname={} file={} scores={} nsfw_ratio={} triggered={} cache={}",
                task.getGroupId(), task.getMessageId(), task.getUserId(), task.getSenderNickname(),
                fileName, scores, String.format("%.3f", nsfwRatio), triggered, cache);
    }

    /**
     * 日志用相对路径：{@code {群号}/{yyyyMMdd}/{文件名}}，未落盘时为 {@code "-"}。
     */
    private static String formatSavedImageName(File file) {
        if (file == null) {
            return "-";
        }
        File dayDir = file.getParentFile();
        if (dayDir == null) {
            return file.getName();
        }
        File groupDir = dayDir.getParentFile();
        if (groupDir == null) {
            return dayDir.getName() + "/" + file.getName();
        }
        return groupDir.getName() + "/" + dayDir.getName() + "/" + file.getName();
    }

    /**
     * 将群消息中的可审图片任务放入队列（非阻塞）。
     * ONNX 未就绪、发送者在白名单、或解析不到图片段时直接返回。
     */
    public void enqueue(CoolQ cq, CQGroupMessageEvent event, GroupModerationItem groupConfig) {
        if (!config.isEnable() || !nsfwDetector.isReady()) {
            return;
        }
        if (moderationActionService.isExempt(groupConfig, event.getSender())) {
            return;
        }
        List<CqImageParser.CqImageSegment> segments = CqImageParser.parseAll(event.getMessage());
        if (segments.isEmpty()) {
            return;
        }

        CQGroupUser sender = event.getSender();
        ModerationTask task = ModerationTask.builder()
                .selfId(cq.getSelfId())
                .messageId(event.getMessageId())
                .groupId(event.getGroupId())
                .userId(event.getUserId())
                .senderNickname(sender != null ? sender.getNickname() : null)
                .imageSegments(segments)
                .enqueuedAt(System.currentTimeMillis())
                .build();

        if (!queue.offer(task)) {
            log.warn("群审队列已满，丢弃 messageId={} groupId={}", task.getMessageId(), task.getGroupId());
        }
    }

    /**
     * 消费单条审核任务：逐图识别 → 汇总最高命中 → 撤回 / 禁言 / 告警。
     * 同一 messageId 只撤回一次（整条消息），多图时以分数最高的命中图为准。
     */
    private void processTask(ModerationTask task) {
        CoolQ cq = CQGlobal.robots.get(task.getSelfId());
        if (cq == null) {
            log.warn("机器人不可用 selfId={} messageId={}", task.getSelfId(), task.getMessageId());
            return;
        }

        Optional<GroupModerationItem> groupOpt = config.findGroup(task.getGroupId());
        if (!groupOpt.isPresent()) {
            log.warn("群审任务放弃 messageId={} groupId={} userId={} reason=group_not_configured",
                    task.getMessageId(), task.getGroupId(), task.getUserId());
            return;
        }
        GroupModerationItem groupConfig = groupOpt.get();

        TriggerResult bestTrigger = TriggerResult.notTriggered();
        String bestScores = null;
        File notifyImage = null;
        File bestSavedImage = null;
        List<File> savedFiles = new ArrayList<>();

        for (CqImageParser.CqImageSegment segment : task.getImageSegments()) {
            try {
                ImageHit hit = inspectImage(cq, task, segment, groupConfig);
                if (!hit.trigger.isTriggered()) {
                    continue;
                }
                File savedFile = null;
                if (groupConfig.isSaveEnable() && hit.imageBytes != null) {
                    savedFile = imageStorageService.save(
                            task.getGroupId(), task.getUserId(), task.getMessageId(), hit.imageBytes,
                            segmentDisplayName(segment), segment.getUrl());
                    savedFiles.add(savedFile);
                    log.info("群审图片已保存 messageId={} groupId={} userId={} savedImage={}",
                            task.getMessageId(), task.getGroupId(), task.getUserId(),
                            formatSavedImageName(savedFile));
                }
                if (notifyImage == null && hit.imageBytes != null) {
                    notifyImage = savedFile != null
                            ? savedFile
                            : imageStorageService.saveToTemp(hit.imageBytes, segmentDisplayName(segment), segment.getUrl());
                }
                if (!bestTrigger.isTriggered() || hit.trigger.getScore() > bestTrigger.getScore()) {
                    bestTrigger = hit.trigger;
                    bestScores = hit.scores;
                    bestSavedImage = savedFile;
                }
            } catch (Exception e) {
                log.error("群审单图异常 messageId={}", task.getMessageId(), e);
            }
        }

        if (!bestTrigger.isTriggered()) {
            return;
        }


        if (groupConfig.isRecallEnable()) {
            boolean recalled = moderationActionService.recall(
                    cq, task.getMessageId(), task.getGroupId(), task.getUserId(), task.getSenderNickname(),
                    bestTrigger, bestScores, formatSavedImageName(bestSavedImage));
            if (!recalled) {
                moderationActionService.replyRecallFailed(
                        cq, task.getGroupId(), task.getMessageId(), bestTrigger, bestScores,
                        formatSavedImageName(bestSavedImage));
            }
        }
        if (groupConfig.isBanEnable() && moderationActionService.botCanBan(cq, task.getGroupId())) {
            moderationActionService.ban(cq, task.getGroupId(), task.getUserId(), groupConfig.getBanDurationSeconds());
        }
        if (moderationActionService.shouldNotify(groupConfig)) {
            File fileForNotify = notifyImage != null ? notifyImage : (savedFiles.isEmpty() ? null : savedFiles.get(0));
            moderationActionService.notifyGroup(
                    cq, groupConfig, task.getGroupId(), task.getUserId(), task.getSenderNickname(),
                    bestTrigger, bestScores != null ? bestScores : "", fileForNotify,
                    formatSavedImageName(bestSavedImage));
            if (!groupConfig.isSaveEnable() && fileForNotify != null) {
                fileForNotify.delete();
            }
        }
    }

    /**
     * 单张图片：拉取 → 查缓存 → ONNX 推理 → 判定是否命中。
     * 缓存命中时仍拉取字节（供保存/告警），但跳过 {@link GantManOnnxNsfwDetector#predict}。
     */
    private ImageHit inspectImage(CoolQ cq, ModerationTask task, CqImageParser.CqImageSegment segment,
                                  GroupModerationItem groupConfig) throws Exception {
        String fileName = segmentDisplayName(segment);
        byte[] imageBytes = imageFetchService.fetch(cq, segment).orElse(null);
        if (imageBytes == null || imageBytes.length == 0) {
            log.warn("群审识别失败 groupId={} messageId={} userId={} nickname={} file={} reason=fetch_empty",
                    task.getGroupId(), task.getMessageId(), task.getUserId(), task.getSenderNickname(), fileName);
            return ImageHit.pass();
        }

        String cacheKey = sha256(imageBytes);
        CachedHit cached = recentCache.get(cacheKey);
        if (cached != null) {
            logRecognition(task, fileName, cached.scores, cached.nsfwRatio, true, true);
            return new ImageHit(cached.trigger, cached.scores, imageBytes);
        }

        NsfwPrediction prediction = nsfwDetector.predict(imageBytes);
        TriggerResult trigger = prediction.evaluateAgainst(groupConfig.getNsfwRatioThreshold());
        String scores = prediction.formatTopScores();
        float nsfwRatio = prediction.getNsfwRatio();
        logRecognition(task, fileName, scores, nsfwRatio, trigger.isTriggered(), false);
        if (!trigger.isTriggered()) {
            return ImageHit.pass();
        }
        recentCache.put(cacheKey, new CachedHit(trigger, scores, nsfwRatio));
        return new ImageHit(trigger, scores, imageBytes);
    }

    /** 缓存条目：上次 ONNX 推理的分数、加权 nsfw 比例与是否违规。 */
    private static class CachedHit {
        final TriggerResult trigger;
        final String scores;
        final float nsfwRatio;

        CachedHit(TriggerResult trigger, String scores, float nsfwRatio) {
            this.trigger = trigger;
            this.scores = scores;
            this.nsfwRatio = nsfwRatio;
        }
    }

    /** 单图识别与处置中间结果。 */
    private static class ImageHit {
        final TriggerResult trigger;
        final String scores;
        final byte[] imageBytes;

        ImageHit(TriggerResult trigger, String scores, byte[] imageBytes) {
            this.trigger = trigger;
            this.scores = scores;
            this.imageBytes = imageBytes;
        }

        static ImageHit pass() {
            return new ImageHit(TriggerResult.notTriggered(), null, null);
        }
    }
}
