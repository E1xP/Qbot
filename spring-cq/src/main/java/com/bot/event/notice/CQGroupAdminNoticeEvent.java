package com.bot.event.notice;

import com.alibaba.fastjson.annotation.JSONField;
import com.bot.event.enums.GroupAdminSubType;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 群管理员变动
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CQGroupAdminNoticeEvent extends CQNoticeEvent {
    /**
     * 事件子类型
     * set unset分别表示设置和取消管理员
     */
    @JSONField(name = "sub_type")
    private GroupAdminSubType subType;
    /**
     * 群号
     */
    @JSONField(name = "group_id")
    private long groupId;
}
