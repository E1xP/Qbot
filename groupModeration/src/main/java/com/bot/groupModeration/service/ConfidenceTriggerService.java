package com.bot.groupModeration.service;

import com.bot.groupModeration.pojo.GroupModerationItem;
import com.bot.groupModeration.pojo.NsfwPrediction;
import com.bot.groupModeration.pojo.TriggerResult;
import org.springframework.stereotype.Service;

/**
 * 置信度触发：hentai+porn+sexy 加权分之和 / 五类加权分之和，达到阈值则命中。
 */
@Service
public class ConfidenceTriggerService {

    private static final String NSFW_RATIO_LABEL = "nsfw_ratio";

    /**
     * @param prediction  模型输出
     * @param groupConfig 群配置
     * @return 未命中时 {@link TriggerResult#notTriggered()}
     */
    public TriggerResult evaluate(NsfwPrediction prediction, GroupModerationItem groupConfig) {
        float ratio = prediction.getNsfwRatio();
        double threshold = groupConfig.getNsfwRatioThreshold();
        if (ratio >= threshold) {
            return TriggerResult.of(NSFW_RATIO_LABEL, ratio, threshold);
        }
        return TriggerResult.notTriggered();
    }
}
