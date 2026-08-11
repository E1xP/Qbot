package com.bot.event.enums;

import com.alibaba.fastjson.annotation.JSONCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 生命周期元事件 sub_type。
 */
@Getter
@AllArgsConstructor
public enum LifecycleSubType implements OneBotSubType {

    /**
     * 插件启用
     */
    ENABLE("enable"),
    /**
     * 插件停用
     */
    DISABLE("disable"),
    /**
     * WebSocket 连接成功
     */
    CONNECT("connect");

    private final String value;

    @JSONCreator
    public static LifecycleSubType fromValue(String value) {
        return OneBotSubTypeParser.parse(LifecycleSubType.class, value);
    }
}
