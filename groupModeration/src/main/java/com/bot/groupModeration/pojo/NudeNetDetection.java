package com.bot.groupModeration.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * NudeNet 单条检测结果（label + 置信度 + 相对原图面积比）。
 */
@Data
@AllArgsConstructor
public class NudeNetDetection {

    private String label;
    private float score;
    /**
     * 检测框与原图交集面积 / 原图面积，范围 [0, 1]
     */
    private float areaRatio;
}
