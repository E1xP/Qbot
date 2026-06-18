package com.bot.event.request;

import com.alibaba.fastjson.annotation.JSONField;
import com.bot.event.enums.GroupRequestSubType;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 加群请求事件
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CQGroupRequestEvent extends CQRequestEvent {
    /**
     * 请求子类型：add 表示用户加群申请，invite 表示邀请机器人入群
     */
    @JSONField(name = "sub_type")
    private GroupRequestSubType subType;
    /**
     * 群号
     */
    @JSONField(name = "group_id")
    private long groupId;
}
