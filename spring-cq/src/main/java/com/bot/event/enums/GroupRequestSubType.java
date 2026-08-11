package com.bot.event.enums;

import com.alibaba.fastjson.annotation.JSONCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 加群请求事件 sub_type。
 */
@Getter
@AllArgsConstructor
public enum GroupRequestSubType implements OneBotSubType {

    /**
     * 用户加群申请
     */
    ADD("add"),
    /**
     * 邀请机器人入群
     */
    INVITE("invite");

    private final String value;

    @JSONCreator
    public static GroupRequestSubType fromValue(String value) {
        return OneBotSubTypeParser.parse(GroupRequestSubType.class, value);
    }
}
