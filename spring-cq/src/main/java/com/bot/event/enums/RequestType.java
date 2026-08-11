package com.bot.event.enums;

import com.alibaba.fastjson.annotation.JSONCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 请求事件 request_type。
 */
@Getter
@AllArgsConstructor
public enum RequestType implements OneBotSubType {

    /**
     * 加好友请求
     */
    FRIEND("friend"),
    /**
     * 加群请求/邀请
     */
    GROUP("group");

    private final String value;

    @JSONCreator
    public static RequestType fromValue(String value) {
        return OneBotSubTypeParser.parse(RequestType.class, value);
    }
}
