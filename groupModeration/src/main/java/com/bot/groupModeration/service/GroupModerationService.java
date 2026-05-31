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
 * 群审：入队立即返回；单 worker 顺序推理，命中后按 messageId 撤回/禁言/告警。
 */
@Service
@Slf4j
public class GroupModerationService {

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
    private ConfidenceTriggerService confidenceTriggerService;
    @Resource
    private ModerationActionService moderationActionService;
    private BlockingQueue<ModerationTask> queue;
    private ExecutorService worker;

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

    private void processTask(ModerationTask task) {
        CoolQ cq = CQGlobal.robots.get(task.getSelfId());
        if (cq == null) {
            log.warn("机器人不可用 selfId={} messageId={}", task.getSelfId(), task.getMessageId());
            return;
        }

        Optional<GroupModerationItem> groupOpt = config.findGroup(task.getGroupId());
        if (!groupOpt.isPresent()) {
            return;
        }
        GroupModerationItem groupConfig = groupOpt.get();

        TriggerResult bestTrigger = TriggerResult.notTriggered();
        String bestScores = null;
        File notifyImage = null;
        List<File> savedFiles = new ArrayList<>();

        for (CqImageParser.CqImageSegment segment : task.getImageSegments()) {
            try {
                ImageHit hit = inspectImage(cq, segment, groupConfig);
                if (!hit.trigger.isTriggered()) {
                    continue;
                }
                if (!bestTrigger.isTriggered() || hit.trigger.getScore() > bestTrigger.getScore()) {
                    bestTrigger = hit.trigger;
                    bestScores = hit.scores;
                }
                if (groupConfig.isSaveEnable() && hit.imageBytes != null) {
                    savedFiles.add(imageStorageService.save(
                            task.getGroupId(), task.getUserId(), task.getMessageId(), hit.imageBytes,
                            segment.getFile(), segment.getUrl()));
                }
                if (notifyImage == null && hit.imageBytes != null) {
                    notifyImage = !savedFiles.isEmpty()
                            ? savedFiles.get(savedFiles.size() - 1)
                            : imageStorageService.saveToTemp(hit.imageBytes, segment.getFile(), segment.getUrl());
                }
            } catch (Exception e) {
                log.error("群审单图异常 messageId={}", task.getMessageId(), e);
            }
        }

        if (!bestTrigger.isTriggered()) {
            return;
        }

        log.info("群审命中 messageId={} groupId={} {}={}",
                task.getMessageId(), task.getGroupId(), bestTrigger.getLabel(), bestTrigger.getScore());

        if (groupConfig.isRecallEnable()) {
            boolean recalled = moderationActionService.recall(cq, task.getMessageId());
            if (!recalled) {
                moderationActionService.replyRecallFailed(
                        cq, task.getGroupId(), task.getMessageId(), bestTrigger, bestScores);
            }
        }
        if (groupConfig.isBanEnable() && moderationActionService.botCanBan(cq, task.getGroupId())) {
            moderationActionService.ban(cq, task.getGroupId(), task.getUserId(), groupConfig.getBanDurationSeconds());
        }
        if (moderationActionService.shouldNotify(groupConfig)) {
            File fileForNotify = notifyImage != null ? notifyImage : (savedFiles.isEmpty() ? null : savedFiles.get(0));
            moderationActionService.notifyGroup(
                    cq, groupConfig, task.getGroupId(), task.getUserId(), task.getSenderNickname(),
                    bestTrigger, bestScores != null ? bestScores : "", fileForNotify);
            if (!groupConfig.isSaveEnable() && fileForNotify != null) {
                fileForNotify.delete();
            }
        }
    }

    private ImageHit inspectImage(CoolQ cq, CqImageParser.CqImageSegment segment,
                                  GroupModerationItem groupConfig) throws Exception {
        byte[] imageBytes = imageFetchService.fetch(cq, segment).orElse(null);
        if (imageBytes == null || imageBytes.length == 0) {
            return ImageHit.pass();
        }

        String cacheKey = sha256(imageBytes);
        CachedHit cached = recentCache.get(cacheKey);
        if (cached != null) {
            return cached.trigger.isTriggered()
                    ? new ImageHit(cached.trigger, cached.scores, imageBytes)
                    : ImageHit.pass();
        }

        NsfwPrediction prediction = nsfwDetector.predict(imageBytes);
        TriggerResult trigger = confidenceTriggerService.evaluate(prediction, groupConfig);
        if (!trigger.isTriggered()) {
            return ImageHit.pass();
        }
        String scores = prediction.formatTopScores();
        recentCache.put(cacheKey, new CachedHit(trigger, scores));
        return new ImageHit(trigger, scores, imageBytes);
    }

    private static class CachedHit {
        final TriggerResult trigger;
        final String scores;

        CachedHit(TriggerResult trigger, String scores) {
            this.trigger = trigger;
            this.scores = scores;
        }
    }

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
