package com.bot.event.enums;

import com.alibaba.fastjson.annotation.JSONCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 上报事件 post_type。
 */
@Getter
@AllArgsConstructor
public enum PostType implements OneBotSubType {

    /**
     * 消息事件
     */
    MESSAGE("message"),
    /**
     * 机器人主动发送的消息事件（go-cqhttp 扩展）
     */
    MESSAGE_SENT("message_sent"),
    /**
     * 通知事件
     */
    NOTICE("notice"),
    /**
     * 请求事件
     */
    REQUEST("request"),
    /**
     * 元事件
     */
    META_EVENT("meta_event");

    private final String value;

    @JSONCreator
    public static PostType fromValue(String value) {
        return OneBotSubTypeParser.parse(PostType.class, value);
    }
}
