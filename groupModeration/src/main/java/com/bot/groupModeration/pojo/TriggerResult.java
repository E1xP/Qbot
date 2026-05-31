package com.bot.groupModeration.pojo;

import lombok.Data;

/**
 * 规则评估结果：是否触发处置及用于日志/告警的分数与阈值。
 * <p>
 * 精判 ban 时 {@link #label} 形如 {@code nudenet:REASON}，{@link #score} 为触发的聚合分。
 */
@Data
public class TriggerResult {

    /**
     * 是否触发处置流程
     */
    private boolean triggered;

    /**
     * 命中的标签名
     */
    private String label;

    /**
     * 该标签模型输出分数
     */
    private float score;

    /**
     * 触发时使用的阈值（来自配置）
     */
    private double threshold;

    public static TriggerResult notTriggered() {
        TriggerResult r = new TriggerResult();
        r.triggered = false;
        return r;
    }

    public static TriggerResult of(String label, float score, double threshold) {
        TriggerResult r = new TriggerResult();
        r.triggered = true;
        r.label = label;
        r.score = score;
        r.threshold = threshold;
        return r;
    }
}
