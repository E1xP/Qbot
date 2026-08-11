package com.bot.event.enums;

import com.alibaba.fastjson.annotation.JSONCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息事件 message_type。
 */
@Getter
@AllArgsConstructor
public enum MessageType implements OneBotSubType {

    /**
     * 私聊消息
     */
    PRIVATE("private"),
    /**
     * 群消息
     */
    GROUP("group"),
    /**
     * 讨论组消息
     */
    DISCUSS("discuss");

    private final String value;

    @JSONCreator
    public static MessageType fromValue(String value) {
        return OneBotSubTypeParser.parse(MessageType.class, value);
    }
}
