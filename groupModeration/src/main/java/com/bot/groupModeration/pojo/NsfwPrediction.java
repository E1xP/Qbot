package com.bot.groupModeration.pojo;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ONNX 模型单次推理结果（GantMan 五分类 softmax 概率）。
 * <p>
 * 加权 NSFW 比例：分子 = hentai+porn+sexy 的 Σ(概率×权重)，分母 = 五类 Σ(概率×权重)，
 * 权重见静态块 {@link #WEIGHTS}。
 */
@Data
public class NsfwPrediction {

    /**
     * 与 GantMan/nsfw_model 输出顺序一致的标签名
     */
    public static final String[] LABELS = {"drawings", "hentai", "neutral", "porn", "sexy"};

    /**
     * 分子参与标签：hentai、porn、sexy
     */
    public static final String[] NSFW_LABELS = {"hentai", "porn", "sexy"};

    /**
     * 五类权重：drawings=100, hentai=95, neutral=100, porn=150, sexy=85
     */
    private static final Map<String, Float> WEIGHTS = new LinkedHashMap<>();

    static {
        WEIGHTS.put("drawings", 100f);
        WEIGHTS.put("hentai", 95f);
        WEIGHTS.put("neutral", 100f);
        WEIGHTS.put("porn", 150f);
        WEIGHTS.put("sexy", 85f);
    }

    /** 标签 → 概率（0～1） */
    private final Map<String, Float> scores = new LinkedHashMap<>();

    public float getScore(String label) {
        return scores.getOrDefault(label.toLowerCase(), 0f);
    }

    /**
     * 单类加权分：p × w
     */
    public float getWeightedScore(String label) {
        float weight = WEIGHTS.getOrDefault(label.toLowerCase(), 0f);
        return getScore(label) * weight;
    }

    /**
     * 分子 = hentai+porn+sexy 的 Σ(p×w)；分母 = 五类 Σ(p×w)。
     */
    public float getNsfwRatio() {
        float numerator = 0f;
        for (String label : NSFW_LABELS) {
            numerator += getWeightedScore(label);
        }
        float denominator = 0f;
        for (String label : LABELS) {
            denominator += getWeightedScore(label);
        }
        return denominator <= 0f ? 0f : numerator / denominator;
    }

    /**
     * 格式化日志用，例如 porn=0.912, hentai=0.043, ...
     */
    public String formatTopScores() {
        StringBuilder sb = new StringBuilder();
        for (String label : LABELS) {
            if (scores.containsKey(label)) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(label).append("=").append(String.format("%.3f", scores.get(label)));
            }
        }
        return sb.toString();
    }

    /**
     * 初筛置信度（0～100）：{@link #getNsfwRatio()} × 100。
     */
    public float getConfidencePercent() {
        return getNsfwRatio() * 100f;
    }

    /**
     * 初筛是否进入 NudeNet 精判。
     */
    public boolean passesPrescreen(double thresholdPercent) {
        return getConfidencePercent() >= thresholdPercent;
    }
}
