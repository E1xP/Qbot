package com.bot.event.enums;

import com.alibaba.fastjson.annotation.JSONCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 群管理员变动通知 sub_type。
 */
@Getter
@AllArgsConstructor
public enum GroupAdminSubType implements OneBotSubType {

    /**
     * 设置管理员
     */
    SET("set"),
    /**
     * 取消管理员
     */
    UNSET("unset");

    private final String value;

    @JSONCreator
    public static GroupAdminSubType fromValue(String value) {
        return OneBotSubTypeParser.parse(GroupAdminSubType.class, value);
    }
}
