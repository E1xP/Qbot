package com.bot.event.enums;

import com.alibaba.fastjson.annotation.JSONCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 群成员减少通知 sub_type。
 */
@Getter
@AllArgsConstructor
public enum GroupDecreaseSubType implements OneBotSubType {

    /**
     * 主动退群
     */
    LEAVE("leave"),
    /**
     * 成员被踢
     */
    KICK("kick"),
    /**
     * 机器人被踢
     */
    KICK_ME("kick_me");

    private final String value;

    @JSONCreator
    public static GroupDecreaseSubType fromValue(String value) {
        return OneBotSubTypeParser.parse(GroupDecreaseSubType.class, value);
    }
}
