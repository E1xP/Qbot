package com.bot.groupModeration.service;

import com.bot.CQGlobal;
import com.bot.entity.CQGroupUser;
import com.bot.event.message.CQGroupMessageEvent;
import com.bot.groupModeration.config.GroupModerationConfig;
import com.bot.groupModeration.detector.GantManOnnxNsfwDetector;
import com.bot.groupModeration.detector.NudeNetOnnxDetector;
import com.bot.groupModeration.pojo.*;
import com.bot.groupModeration.util.CqImageParser;
import com.bot.robot.CoolQ;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.File;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

/**
 * 群审调度：初筛队列 + 精判队列，各单 worker；命中后 {@link ModerationActionService} 处置。
 */
@Service
@Slf4j
public class GroupModerationService {

    private final Map<String, ModerationVerdict> resultCache = new ConcurrentHashMap<>();

    @Resource
    private GroupModerationConfig config;
    @Resource
    private GantManOnnxNsfwDetector prescreenDetector;
    @Resource
    private NudeNetOnnxDetector refineDetector;
    @Resource
    private ImageFetchService imageFetchService;
    @Resource
    private ImageStorageService imageStorageService;
    @Resource
    private ModerationActionService moderationActionService;

    private BlockingQueue<ModerationTask> prescreenQueue;
    private BlockingQueue<RefineTask> refineQueue;
    private ExecutorService prescreenWorker;
    private ExecutorService refineWorker;

    private static void shutdownWorker(ExecutorService worker) {
        if (worker != null) {
            worker.shutdownNow();
            try {
                worker.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static String digest(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static void logVerdict(ModerationTask task, String fileName, String stage,
                                   ModerationVerdict verdict, boolean cache) {
        log.info("群审识别 stage={} groupId={} messageId={} userId={} nickname={} file={} confidence={}% refined={} triggered={} cache={} scores={} refine={}",
                stage, task.getGroupId(), task.getMessageId(), task.getUserId(), task.getSenderNickname(),
                fileName, String.format(Locale.ROOT, "%.1f", verdict.getPrescreenConfidence()),
                verdict.isRefined(), verdict.isTriggered(), cache, verdict.getPrescreenScores(),
                verdict.isRefined() ? verdict.getRefineSummary() : "-");
    }

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

    @PostConstruct
    public void startWorkers() {
        int prescreenCap = Math.max(16, config.getTaskQueueCapacity());
        int refineCap = Math.max(16, config.getRefineQueueCapacity());
        prescreenQueue = new LinkedBlockingQueue<>(prescreenCap);
        refineQueue = new LinkedBlockingQueue<>(refineCap);

        prescreenWorker = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "group-moderation-prescreen");
            t.setDaemon(true);
            return t;
        });
        refineWorker = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "group-moderation-refine");
            t.setDaemon(true);
            return t;
        });

        prescreenWorker.submit(this::prescreenLoop);
        refineWorker.submit(this::refineLoop);
        log.info("群审队列已启动 prescreenCapacity={} refineCapacity={} prescreenThreshold={}%",
                prescreenCap, refineCap, config.getPrescreenThreshold());
    }

    @PreDestroy
    public void stopWorkers() {
        shutdownWorker(prescreenWorker);
        shutdownWorker(refineWorker);
    }

    public void enqueue(CoolQ cq, CQGroupMessageEvent event, GroupModerationItem groupConfig) {
        if (!config.isEnable() || !isReady()) {
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

        if (!prescreenQueue.offer(task)) {
            log.warn("初筛队列已满，丢弃 messageId={} groupId={}", task.getMessageId(), task.getGroupId());
        }
    }

    private boolean isReady() {
        return prescreenDetector.isReady() && refineDetector.isReady();
    }

    private void prescreenLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                runPrescreen(prescreenQueue.take());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("初筛消费异常", e);
            }
        }
    }

    private void refineLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                runRefine(refineQueue.take());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("精判消费异常", e);
            }
        }
    }

    private void runPrescreen(ModerationTask task) throws Exception {
        CoolQ cq = CQGlobal.robots.get(task.getSelfId());
        if (cq == null) {
            log.warn("机器人不可用 selfId={} messageId={}", task.getSelfId(), task.getMessageId());
            return;
        }
        Optional<GroupModerationItem> groupOpt = config.findGroup(task.getGroupId());
        if (!groupOpt.isPresent()) {
            log.warn("群审任务放弃 messageId={} groupId={} reason=group_not_configured", task.getMessageId(), task.getGroupId());
            return;
        }
        GroupModerationItem groupConfig = groupOpt.get();
        double prescreenThreshold = groupConfig.resolvePrescreenThresholdPercent(config.getPrescreenThreshold());

        List<RefineTask.DoneImage> done = new ArrayList<>();
        List<RefineTask.PendingImage> pending = new ArrayList<>();

        for (CqImageParser.CqImageSegment segment : task.getImageSegments()) {
            String fileName = CqImageParser.displayName(segment);
            byte[] imageBytes = imageFetchService.fetch(cq, segment).orElse(null);
            if (imageBytes == null || imageBytes.length == 0) {
                log.warn("群审识别失败 stage=prescreen groupId={} messageId={} file={} reason=fetch_empty",
                        task.getGroupId(), task.getMessageId(), fileName);
                continue;
            }

            Optional<ModerationVerdict> cached = cacheGet(imageBytes);
            if (cached.isPresent()) {
                ModerationVerdict verdict = cached.get().withImageBytes(imageBytes);
                logVerdict(task, fileName, "prescreen", verdict, true);
                done.add(new RefineTask.DoneImage(segment, verdict));
                continue;
            }

            NsfwPrediction prediction = prescreenDetector.predict(imageBytes);
            float confidence = prediction.getConfidencePercent();
            String scores = prediction.formatTopScores();

            if (!prediction.passesPrescreen(prescreenThreshold)) {
                ModerationVerdict verdict = ModerationVerdict.prescreenPass(scores, confidence, imageBytes);
                cachePut(imageBytes, verdict);
                logVerdict(task, fileName, "prescreen", verdict, false);
                done.add(new RefineTask.DoneImage(segment, verdict));
                continue;
            }

            log.info("群审初筛过线 groupId={} messageId={} file={} confidence={}% → 入精判队列",
                    task.getGroupId(), task.getMessageId(), fileName,
                    String.format(Locale.ROOT, "%.1f", confidence));
            pending.add(RefineTask.PendingImage.builder()
                    .segment(segment)
                    .imageBytes(imageBytes)
                    .prescreenConfidence(confidence)
                    .prescreenScores(scores)
                    .build());
        }

        if (pending.isEmpty()) {
            applyActions(cq, task, groupConfig, done);
            return;
        }

        RefineTask refineTask = RefineTask.builder()
                .selfId(task.getSelfId())
                .messageId(task.getMessageId())
                .groupId(task.getGroupId())
                .userId(task.getUserId())
                .senderNickname(task.getSenderNickname())
                .pending(pending)
                .prescreenDone(done)
                .build();
        if (!refineQueue.offer(refineTask)) {
            log.warn("精判队列已满，丢弃 messageId={} groupId={} pendingImages={}",
                    task.getMessageId(), task.getGroupId(), pending.size());
        }
    }

    private void runRefine(RefineTask task) throws Exception {
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

        List<RefineTask.DoneImage> all = new ArrayList<>(task.getPrescreenDone());
        ModerationTask messageTask = ModerationTask.builder()
                .selfId(task.getSelfId())
                .messageId(task.getMessageId())
                .groupId(task.getGroupId())
                .userId(task.getUserId())
                .senderNickname(task.getSenderNickname())
                .build();

        for (RefineTask.PendingImage pending : task.getPending()) {
            String fileName = CqImageParser.displayName(pending.getSegment());
            byte[] imageBytes = pending.getImageBytes();
            ModerationVerdict verdict;

            Optional<ModerationVerdict> cached = cacheGet(imageBytes);
            if (cached.isPresent() && cached.get().isRefined()) {
                verdict = cached.get().withImageBytes(imageBytes);
                logVerdict(messageTask, fileName, "refine", verdict, true);
            } else {
                NudeNetOnnxDetector.JudgeResult judge = refineDetector.judge(imageBytes);
                verdict = ModerationVerdict.of(
                        pending.getPrescreenScores(), pending.getPrescreenConfidence(),
                        true, judge.getSummary(), judge.getTrigger(), imageBytes);
                cachePut(imageBytes, verdict);
                logVerdict(messageTask, fileName, "refine", verdict, false);
            }
            all.add(new RefineTask.DoneImage(pending.getSegment(), verdict));
        }

        applyActions(cq, messageTask, groupConfig, all);
    }

    private void applyActions(CoolQ cq, ModerationTask task, GroupModerationItem groupConfig,
                              List<RefineTask.DoneImage> results) {
        TriggerResult bestTrigger = TriggerResult.notTriggered();
        String bestScores = null;
        File notifyImage = null;
        File bestSavedImage = null;
        List<File> savedFiles = new ArrayList<>();

        for (RefineTask.DoneImage item : results) {
            ModerationVerdict verdict = item.getVerdict();
            if (!verdict.isTriggered()) {
                continue;
            }
            CqImageParser.CqImageSegment segment = item.getSegment();
            File savedFile = null;
            if (groupConfig.isSaveEnable() && verdict.getImageBytes() != null) {
                savedFile = imageStorageService.save(
                        task.getGroupId(), task.getUserId(), task.getMessageId(), verdict.getImageBytes(),
                        CqImageParser.displayName(segment), segment.getUrl());
                savedFiles.add(savedFile);
                log.info("群审图片已保存 messageId={} groupId={} userId={} savedImage={}",
                        task.getMessageId(), task.getGroupId(), task.getUserId(),
                        formatSavedImageName(savedFile));
            }
            if (notifyImage == null && verdict.getImageBytes() != null) {
                notifyImage = savedFile != null
                        ? savedFile
                        : imageStorageService.saveToTemp(
                        verdict.getImageBytes(), CqImageParser.displayName(segment), segment.getUrl());
            }
            if (!bestTrigger.isTriggered() || verdict.getTrigger().getScore() > bestTrigger.getScore()) {
                bestTrigger = verdict.getTrigger();
                bestScores = verdict.actionSummary();
                bestSavedImage = savedFile;
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

    private Optional<ModerationVerdict> cacheGet(byte[] imageBytes) throws Exception {
        return Optional.ofNullable(resultCache.get(digest(imageBytes)));
    }

    private void cachePut(byte[] imageBytes, ModerationVerdict verdict) throws Exception {
        resultCache.put(digest(imageBytes), verdict.withoutImageBytes());
    }
}
