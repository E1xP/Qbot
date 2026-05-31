package com.bot.utils;

import com.bot.entity.CQGroupUser;
import com.bot.retdata.GroupMemberInfoData;

/**
 * 群内展示名：优先群名片，未设置时回退 QQ 昵称。
 */
public final class GroupUserNames {

    private GroupUserNames() {
    }

    public static String displayName(CQGroupUser sender) {
        if (sender == null) {
            return null;
        }
        return displayName(sender.getCard(), sender.getNickname());
    }

    public static String displayName(GroupMemberInfoData member) {
        if (member == null) {
            return null;
        }
        return displayName(member.getCard(), member.getNickname());
    }

    public static String displayName(String card, String nickname) {
        if (card != null && !card.isEmpty()) {
            return card;
        }
        return nickname;
    }
}
