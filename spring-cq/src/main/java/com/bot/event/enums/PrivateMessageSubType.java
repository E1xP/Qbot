package com.bot.event.enums;

import com.alibaba.fastjson.annotation.JSONCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 私聊消息 sub_type。
 */
@Getter
@AllArgsConstructor
public enum PrivateMessageSubType implements OneBotSubType {

    /**
     * 好友私聊
     */
    FRIEND("friend"),
    /**
     * 群临时会话
     */
    GROUP("group"),
    /**
     * 讨论组临时会话
     */
    DISCUSS("discuss");

    private final String value;

    @JSONCreator
    public static PrivateMessageSubType fromValue(String value) {
        return OneBotSubTypeParser.parse(PrivateMessageSubType.class, value);
    }
}
