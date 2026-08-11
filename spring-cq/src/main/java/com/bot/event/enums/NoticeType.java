package com.bot.event.enums;

import com.alibaba.fastjson.annotation.JSONCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通知事件 notice_type。
 */
@Getter
@AllArgsConstructor
public enum NoticeType implements OneBotSubType {

    /**
     * 群文件上传
     */
    GROUP_UPLOAD("group_upload"),
    /**
     * 群管理员变动
     */
    GROUP_ADMIN("group_admin"),
    /**
     * 群成员减少
     */
    GROUP_DECREASE("group_decrease"),
    /**
     * 群成员增加
     */
    GROUP_INCREASE("group_increase"),
    /**
     * 群禁言
     */
    GROUP_BAN("group_ban"),
    /**
     * 好友添加
     */
    FRIEND_ADD("friend_add"),
    /**
     * 群消息撤回
     */
    GROUP_RECALL("group_recall"),
    /**
     * 好友消息撤回
     */
    FRIEND_RECALL("friend_recall");

    private final String value;

    @JSONCreator
    public static NoticeType fromValue(String value) {
        return OneBotSubTypeParser.parse(NoticeType.class, value);
    }
}
