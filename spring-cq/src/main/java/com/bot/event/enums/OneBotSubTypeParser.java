package com.bot.event.enums;

/**
 * 解析 OneBot 协议固定字符串枚举值。
 */
public final class OneBotSubTypeParser {

    private OneBotSubTypeParser() {
    }

    public static <E extends Enum<E> & OneBotSubType> E parse(Class<E> type, String value) {
        if (value == null) {
            return null;
        }
        for (E item : type.getEnumConstants()) {
            if (item.getValue().equals(value)) {
                return item;
            }
        }
        return null;
    }
}
