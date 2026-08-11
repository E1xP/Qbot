package com.bot.event.enums;

import com.alibaba.fastjson.annotation.JSONCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 群禁言通知 sub_type。
 */
@Getter
@AllArgsConstructor
public enum GroupBanSubType implements OneBotSubType {

    /**
     * 禁言
     */
    BAN("ban"),
    /**
     * 解除禁言
     */
    LIFT_BAN("lift_ban");

    private final String value;

    @JSONCreator
    public static GroupBanSubType fromValue(String value) {
        return OneBotSubTypeParser.parse(GroupBanSubType.class, value);
    }
}
