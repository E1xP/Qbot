package com.bot.event.enums;

import com.alibaba.fastjson.annotation.JSONCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 群成员增加通知 sub_type。
 */
@Getter
@AllArgsConstructor
public enum GroupIncreaseSubType implements OneBotSubType {

    /**
     * 管理员同意入群
     */
    APPROVE("approve"),
    /**
     * 管理员邀请入群
     */
    INVITE("invite");

    private final String value;

    @JSONCreator
    public static GroupIncreaseSubType fromValue(String value) {
        return OneBotSubTypeParser.parse(GroupIncreaseSubType.class, value);
    }
}
