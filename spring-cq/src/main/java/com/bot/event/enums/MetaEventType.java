package com.bot.event.enums;

import com.alibaba.fastjson.annotation.JSONCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 元事件 meta_event_type。
 */
@Getter
@AllArgsConstructor
public enum MetaEventType implements OneBotSubType {

    /**
     * 心跳
     */
    HEARTBEAT("heartbeat"),
    /**
     * 生命周期
     */
    LIFECYCLE("lifecycle");

    private final String value;

    @JSONCreator
    public static MetaEventType fromValue(String value) {
        return OneBotSubTypeParser.parse(MetaEventType.class, value);
    }
}
