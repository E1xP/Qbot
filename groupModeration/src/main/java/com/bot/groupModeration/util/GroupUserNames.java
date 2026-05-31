package com.bot.groupModeration.util;

import com.bot.entity.CQGroupUser;

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
        String card = sender.getCard();
        if (card != null && !card.isEmpty()) {
            return card;
        }
        return sender.getNickname();
    }
}
