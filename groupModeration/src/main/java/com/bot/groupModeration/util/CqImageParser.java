package com.bot.groupModeration.util;

import com.bot.utils.CQCode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从群消息字符串中解析 [CQ:image,...] 段，提取 file / url 等参数供下载与审核。
 */
public final class CqImageParser {

    private static final Pattern CQ_IMAGE = Pattern.compile("\\[CQ:image([^\\]]*)]", Pattern.CASE_INSENSITIVE);

    private CqImageParser() {
    }

    /**
     * 消息中是否包含图片 CQ 码
     */
    public static boolean hasImage(String message) {
        return message != null && CQ_IMAGE.matcher(message).find();
    }

    /** 解析消息中全部图片段（一条消息可含多张图） */
    public static List<CqImageSegment> parseAll(String message) {
        if (message == null) {
            return Collections.emptyList();
        }
        Matcher matcher = CQ_IMAGE.matcher(message);
        List<CqImageSegment> list = new ArrayList<>();
        while (matcher.find()) {
            list.add(parseParams(matcher.group(1)));
        }
        return list;
    }

    private static CqImageSegment parseParams(String paramPart) {
        CqImageSegment segment = new CqImageSegment();
        if (paramPart == null || paramPart.isEmpty()) {
            return segment;
        }
        String[] pairs = paramPart.split(",");
        for (String pair : pairs) {
            int eq = pair.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = pair.substring(0, eq).trim();
            String value = CQCode.unescape(pair.substring(eq + 1).trim());
            switch (key) {
                case "file":
                    segment.setFile(value);
                    break;
                case "url":
                    segment.setUrl(value);
                    break;
                default:
                    break;
            }
        }
        return segment;
    }

    /**
     * 单张图片 CQ 参数。
     * <p>
     * go-cqhttp 上报通常同时带 file 与 url；优先用 url 下载，否则通过 get_image(file) 解析。
     */
    @lombok.Data
    public static class CqImageSegment {
        /** CQ 码中的 file 字段（缓存文件名或 URL） */
        private String file;
        /** 可下载地址，或 file:/// 本地路径 */
        private String url;
    }
}
