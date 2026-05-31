package com.bot.groupModeration.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * NudeNet 单条检测结果（label + 置信度）。
 */
@Data
@AllArgsConstructor
public class NudeNetDetection {

    private String label;
    private float score;
}
