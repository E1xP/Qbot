package com.bot.groupModeration.pojo;

import lombok.Getter;

import java.util.Locale;

/**
 * 单张图片审核结论（初筛 / 精判 / 缓存 共用）。
 */
@Getter
public class ModerationVerdict {

    private final float prescreenConfidence;
    private final String prescreenScores;
    /**
     * 初筛已过线、精判未完成（Cache 中跳过重复 Inception）
     */
    private final boolean awaitingRefine;
    private final boolean refined;
    private final String refineSummary;
    private final TriggerResult trigger;
    private final byte[] imageBytes;

    private ModerationVerdict(float prescreenConfidence, String prescreenScores, boolean awaitingRefine,
                              boolean refined, String refineSummary, TriggerResult trigger, byte[] imageBytes) {
        this.prescreenConfidence = prescreenConfidence;
        this.prescreenScores = prescreenScores;
        this.awaitingRefine = awaitingRefine;
        this.refined = refined;
        this.refineSummary = refineSummary;
        this.trigger = trigger;
        this.imageBytes = imageBytes;
    }

    public static ModerationVerdict fetchFailed() {
        return new ModerationVerdict(0f, null, false, false, null, TriggerResult.notTriggered(), null);
    }

    public static ModerationVerdict prescreenPass(String prescreenScores, float confidence, byte[] imageBytes) {
        return new ModerationVerdict(confidence, prescreenScores, false, false, null,
                TriggerResult.notTriggered(), imageBytes);
    }

    /**
     * 初筛过线、待精判；写入 Cache 后重复消息可跳过 Inception。
     */
    public static ModerationVerdict prescreenAwaitRefine(String prescreenScores, float confidence, byte[] imageBytes) {
        return new ModerationVerdict(confidence, prescreenScores, true, false, null,
                TriggerResult.notTriggered(), imageBytes);
    }

    public static ModerationVerdict of(String prescreenScores, float confidence, boolean refined,
                                       String refineSummary, TriggerResult trigger, byte[] imageBytes) {
        return new ModerationVerdict(confidence, prescreenScores, false, refined, refineSummary, trigger, imageBytes);
    }

    public boolean isTriggered() {
        return trigger != null && trigger.isTriggered();
    }

    /**
     * 初筛是否通过（置信度低于阈值，无需精判）。
     */
    public boolean isPrescreenPass(double thresholdPercent) {
        if (awaitingRefine || refined) {
            return false;
        }
        return prescreenConfidence < thresholdPercent;
    }

    /**
     * 精判日志用结果：违规 / 放行（仅 refined 后有意义）。
     */
    public String resolveRefineLogResult() {
        return isTriggered() ? "违规" : "放行";
    }

    /**
     * 日志用审核结果：违规 / 过线 / 放行。
     */
    public String resolveLogResult(double thresholdPercent) {
        if (isTriggered()) {
            return "违规";
        }
        if (awaitingRefine) {
            return "过线";
        }
        if (refined) {
            return "放行";
        }
        return prescreenConfidence < thresholdPercent ? "放行" : "过线";
    }

    /**
     * 撤回 / 告警用摘要
     */
    public String actionSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("prescreen=").append(String.format(Locale.ROOT, "%.1f", prescreenConfidence))
                .append("% scores=").append(prescreenScores);
        if (refined && refineSummary != null) {
            sb.append(" refine=").append(refineSummary);
        }
        return sb.toString();
    }

    /**
     * 写入缓存的副本（不含图片字节）
     */
    public ModerationVerdict withoutImageBytes() {
        return new ModerationVerdict(prescreenConfidence, prescreenScores, awaitingRefine, refined, refineSummary, trigger, null);
    }

    public ModerationVerdict withImageBytes(byte[] imageBytes) {
        return new ModerationVerdict(prescreenConfidence, prescreenScores, awaitingRefine, refined, refineSummary, trigger, imageBytes);
    }
}
