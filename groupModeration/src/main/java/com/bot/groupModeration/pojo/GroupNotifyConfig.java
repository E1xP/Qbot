package com.bot.groupModeration.pojo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 全局告警配置：审核命中后向指定群发送文字说明，并以本地文件路径重新上传图片（转载发送）。
 */
@Data
public class GroupNotifyConfig {

    /**
     * 是否启用全局告警（单群 notify-enable 优先）
     */
    private boolean enable;

    /**
     * 接收告警的群号
     */
    private long targetGroupId;

    /**
     * 告警消息开头 @ 的 QQ 列表
     */
    private List<Long> atUserIds = new ArrayList<>();
}
