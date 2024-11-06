package com.bot.event.message;

import com.alibaba.fastjson.annotation.JSONField;
import com.bot.entity.CQUser;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 讨论组消息
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CQDiscussMessageEvent extends CQMessageEvent {
    /**
     * 讨论组 ID
     */
    @JSONField(name = "discuss_id")
    private long discussId;
    /**
     * 发送人信息
     */
    @JSONField(name = "sender")
    private CQUser sender;
}
