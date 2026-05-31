package com.bot.groupModeration.pojo;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ONNX 模型单次推理结果（GantMan 五分类概率）。
 */
@Data
public class NsfwPrediction {

    /**
     * 与 GantMan/nsfw_model 输出顺序一致的标签名
     */
    public static final String[] LABELS = {"drawings", "hentai", "neutral", "porn", "sexy"};

    /**
     * 参与 NSFW 比例计算的标签（权重均为 1）
     */
    public static final String[] NSFW_LABELS = {"hentai", "porn", "sexy"};

    /**
     * 标签 → 概率（0～1）
     */
    private final Map<String, Float> scores = new LinkedHashMap<>();

    public float getScore(String label) {
        return scores.getOrDefault(label.toLowerCase(), 0f);
    }

    /**
     * hentai、porn、sexy 各按权重 1 求和后，除以五类分数之和（各类权重均为 1）。
     */
    public float getNsfwRatio() {
        float numerator = 0f;
        for (String label : NSFW_LABELS) {
            numerator += getScore(label);
        }
        float denominator = 0f;
        for (String label : LABELS) {
            denominator += getScore(label);
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
}
