package com.bot.event.message;

import com.alibaba.fastjson.annotation.JSONField;
import com.bot.entity.CQUser;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 私聊消息
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CQPrivateMessageEvent extends CQMessageEvent {
    /**
     * 消息子类型
     * 如果是好友则是 friend
     * 如果从群或讨论组来的临时会话则分别是 group、discuss
     */
    @JSONField(name = "sub_type")
    private String subType;
    /**
     * 发送人信息
     */
    @JSONField(name = "sender")
    private CQUser sender;
    /**
     * 接收人id
     */
    @JSONField(name = "target_id")
    private long targetId;
}
