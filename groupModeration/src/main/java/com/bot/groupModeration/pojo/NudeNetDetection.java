package com.bot.groupModeration.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * NudeNet 单条检测结果。
 */
@Data
@AllArgsConstructor
public class NudeNetDetection {

    private String label;
    private float score;
    /**
     * 归一化坐标 [x1, y1, x2, y2]，相对原图 0~1
     */
    private float[] box;
}
