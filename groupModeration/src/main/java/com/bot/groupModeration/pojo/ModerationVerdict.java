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
    private final String refineHits;
    private final String refineAggs;
    private final TriggerResult trigger;
    private final byte[] imageBytes;
    /**
     * 结论来自审核结果缓存；违规时不再落盘、不写临时文件。
     */
    private final boolean fromCache;

    private ModerationVerdict(float prescreenConfidence, String prescreenScores, boolean awaitingRefine,
                              boolean refined, String refineSummary, String refineHits, String refineAggs,
                              TriggerResult trigger, byte[] imageBytes, boolean fromCache) {
        this.prescreenConfidence = prescreenConfidence;
        this.prescreenScores = prescreenScores;
        this.awaitingRefine = awaitingRefine;
        this.refined = refined;
        this.refineSummary = refineSummary;
        this.refineHits = refineHits;
        this.refineAggs = refineAggs;
        this.trigger = trigger;
        this.imageBytes = imageBytes;
        this.fromCache = fromCache;
    }

    public static ModerationVerdict fetchFailed() {
        return new ModerationVerdict(0f, null, false, false, null, null, null,
                TriggerResult.notTriggered(), null, false);
    }

    public static ModerationVerdict prescreenPass(String prescreenScores, float confidence, byte[] imageBytes) {
        return new ModerationVerdict(confidence, prescreenScores, false, false, null, null, null,
                TriggerResult.notTriggered(), imageBytes, false);
    }

    /**
     * 初筛过线、待精判；写入 Cache 后重复消息可跳过 Inception。
     */
    public static ModerationVerdict prescreenAwaitRefine(String prescreenScores, float confidence, byte[] imageBytes) {
        return new ModerationVerdict(confidence, prescreenScores, true, false, null, null, null,
                TriggerResult.notTriggered(), imageBytes, false);
    }

    public static ModerationVerdict of(String prescreenScores, float confidence, boolean refined,
                                       String refineSummary, String refineHits, String refineAggs,
                                       TriggerResult trigger, byte[] imageBytes) {
        return new ModerationVerdict(confidence, prescreenScores, false, refined, refineSummary,
                refineHits, refineAggs, trigger, imageBytes, false);
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

    private static String orDash(String value) {
        return value == null || value.isEmpty() ? "无" : value;
    }

    /**
     * 精判对外文案：触发部位 + 检测/聚合（告警、撤回失败等共用）。
     *
     * @param includeResult 是否包含「精判结论：违规/放行」行
     */
    public String refineActionText(boolean includeResult) {
        if (!refined) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (includeResult) {
            sb.append("精判结论：").append(resolveRefineLogResult()).append('\n');
        }
        if (isTriggered() && trigger != null) {
            String part = resolveRefineTriggerPart();
            if (part != null) {
                sb.append("触发：").append(part).append(' ')
                        .append(String.format(Locale.ROOT, "%.0f%%", trigger.getScore() * 100f))
                        .append('\n');
            }
        }
        sb.append("检测：").append(orDash(resolveRefineHitsForLog())).append('\n');
        sb.append("聚合：").append(orDash(resolveRefineAggsForLog()));
        return sb.toString();
    }

    /**
     * 日志 / 撤回记录用精判摘要。
     */
    public String refineLogSummary() {
        String text = refineActionText(true);
        if (text != null) {
            return text;
        }
        return "prescreen=" + String.format(Locale.ROOT, "%.1f", prescreenConfidence)
                + "% scores=" + prescreenScores;
    }

    /**
     * 处置日志用摘要（精判优先）。
     */
    public String actionSummary() {
        return refineLogSummary();
    }

    private static String extractSummaryToken(String summary, String prefix) {
        if (summary == null) {
            return null;
        }
        int start = summary.indexOf(prefix);
        if (start < 0) {
            return null;
        }
        start += prefix.length();
        int end = summary.indexOf(", ", start);
        return end < 0 ? summary.substring(start) : summary.substring(start, end);
    }

    private static String extractAggsFromSummary(String summary) {
        if (summary == null) {
            return null;
        }
        StringBuilder aggs = new StringBuilder();
        appendAggToken(aggs, summary, "Agg=");
        appendAggToken(aggs, summary, "breastAgg=");
        appendAggToken(aggs, summary, "buttAgg=");
        appendAggToken(aggs, summary, "torso=");
        return aggs.length() == 0 ? null : aggs.toString();
    }

    private static void appendAggToken(StringBuilder aggs, String summary, String token) {
        int idx = 0;
        while ((idx = summary.indexOf(token, idx)) >= 0) {
            int keyStart = summary.lastIndexOf(", ", idx);
            keyStart = keyStart < 0 ? 0 : keyStart + 2;
            int valEnd = summary.indexOf(", ", idx);
            if (valEnd < 0) {
                valEnd = summary.length();
            }
            if (aggs.length() > 0) {
                aggs.append(';');
            }
            aggs.append(summary, keyStart, valEnd);
            idx = valEnd;
        }
    }

    /**
     * 写入缓存的副本（不含图片字节）
     */
    public ModerationVerdict withoutImageBytes() {
        return new ModerationVerdict(prescreenConfidence, prescreenScores, awaitingRefine, refined,
                refineSummary, refineHits, refineAggs, trigger, null, fromCache);
    }

    public ModerationVerdict withImageBytes(byte[] imageBytes) {
        return new ModerationVerdict(prescreenConfidence, prescreenScores, awaitingRefine, refined,
                refineSummary, refineHits, refineAggs, trigger, imageBytes, fromCache);
    }

    /** 标记为缓存命中结论，处置阶段跳过落盘 */
    public ModerationVerdict asCacheHit() {
        return new ModerationVerdict(prescreenConfidence, prescreenScores, awaitingRefine, refined,
                refineSummary, refineHits, refineAggs, trigger, imageBytes, true);
    }

    /**
     * 日志用：各检测框 label=score
     */
    public String resolveRefineHitsForLog() {
        if (refineHits != null) {
            return refineHits;
        }
        return extractSummaryToken(refineSummary, "hits=");
    }

    /**
     * 日志用：各聚合分
     */
    public String resolveRefineAggsForLog() {
        if (refineAggs != null) {
            return refineAggs;
        }
        return extractAggsFromSummary(refineSummary);
    }

    /**
     * 日志用：精判触发部位（中文）
     */
    public String resolveRefineTriggerPart() {
        if (!isTriggered() || trigger == null || trigger.getLabel() == null) {
            return null;
        }
        String label = trigger.getLabel();
        if (label.startsWith("nudenet:")) {
            label = label.substring("nudenet:".length());
        }
        return com.bot.groupModeration.detector.NudeNetBanJudgment.toDisplayTriggerPart(label);
    }
}
