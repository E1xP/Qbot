package com.bot.event.enums;

import com.alibaba.fastjson.annotation.JSONCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 群消息 sub_type。
 */
@Getter
@AllArgsConstructor
public enum GroupMessageSubType implements OneBotSubType {

    /**
     * 正常消息
     */
    NORMAL("normal"),
    /**
     * 匿名消息
     */
    ANONYMOUS("anonymous"),
    /**
     * 系统提示（如「管理员已禁止群内匿名聊天」）
     */
    NOTICE("notice");

    private final String value;

    @JSONCreator
    public static GroupMessageSubType fromValue(String value) {
        return OneBotSubTypeParser.parse(GroupMessageSubType.class, value);
    }
}
