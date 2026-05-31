package com.bot.groupModeration.pojo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 单个群的审核与处置配置。
 */
@Data
public class GroupModerationItem {

    /**
     * 群号
     */
    private long groupId;

    /**
     * 是否对该群启用图片审核
     */
    private boolean enable;

    /**
     * 命中后是否禁言（异步，需机器人为群管）
     */
    private boolean banEnable;

    /**
     * 禁言秒数
     */
    private long banDurationSeconds = 600;

    /**
     * 命中后是否撤回整条触发消息
     */
    private boolean recallEnable;

    /**
     * 命中后是否将图片保存到 storage-base-path/{群号}/ 下
     */
    private boolean saveEnable = true;

    /**
     * 命中后是否向告警群发送通知（含重新上传的图片）
     */
    private boolean notifyEnable;

    /**
     * 本群专用告警群号；&lt;=0 时使用全局 notify.target-group-id
     */
    private long notifyGroupId;

    /**
     * 本群告警时 @ 的 QQ；为空则回退到全局 notify.at-user-ids
     */
    private List<Long> notifyAtUserIds = new ArrayList<>();

    /**
     * 白名单 QQ，不审图、不处置
     */
    private List<Long> exemptUserIds = new ArrayList<>();

    /**
     * 初筛置信度阈值（百分比）。未配置时使用全局 {@code prescreen-threshold}（默认 60）。
     * 兼容旧字段：若配置 0～1 的小数则自动 ×100。
     */
    private double nsfwRatioThreshold = 0.60;

    /**
     * 本群初筛阈值（百分比）；≤0 时使用全局配置。
     */
    public double resolvePrescreenThresholdPercent(double globalThreshold) {
        if (nsfwRatioThreshold <= 0) {
            return globalThreshold;
        }
        return nsfwRatioThreshold <= 1d ? nsfwRatioThreshold * 100d : nsfwRatioThreshold;
    }
}
