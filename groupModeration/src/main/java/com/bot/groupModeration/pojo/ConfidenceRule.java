package com.bot.groupModeration.pojo;

import lombok.Data;

/**
 * 置信度触发规则：当模型输出的某标签分数 &gt;= threshold 时视为命中。
 * <p>
 * 多条规则之间为「或」关系，实际处置时取命中规则中分数最高的一条。
 */
@Data
public class ConfidenceRule {

    /**
     * 模型标签名，须与 GantMan 五分类一致：
     * drawings / hentai / neutral / porn / sexy
     */
    private String label;

    /**
     * 触发阈值，范围建议 0.75～0.95；越高误报越少、漏检越多
     */
    private double threshold = 0.88;
}
