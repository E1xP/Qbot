package com.bot.groupModeration.util;

import com.bot.utils.CQCode;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从群消息解析可审核图片段。
 * <p>
 * 支持 {@code [CQ:image,...]} 与扩展名属于图片类型的 {@code [CQ:file,...]}（依据 name/file/url 后缀判断）。
 * {@link CqImageSegment#fileUpload} 为 true 时，拉取阶段优先走 {@code get_file} API。
 */
public final class CqImageParser {

    private static final Pattern CQ_IMAGE = Pattern.compile("\\[CQ:image([^\\]]*)]", Pattern.CASE_INSENSITIVE);
    private static final Pattern CQ_FILE = Pattern.compile("\\[CQ:file([^\\]]*)]", Pattern.CASE_INSENSITIVE);

    private static final Set<String> IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".jpg", ".jpeg", ".jfif", ".png", ".gif", ".webp", ".bmp"
    ));

    private CqImageParser() {
    }

    /**
     * 消息中是否包含可审核图片（含图片文件上传）
     */
    public static boolean hasImage(String message) {
        return !parseAll(message).isEmpty();
    }

    /** 解析消息中全部可审核图片段 */
    public static List<CqImageSegment> parseAll(String message) {
        if (message == null) {
            return Collections.emptyList();
        }
        List<CqImageSegment> list = new ArrayList<>();
        Matcher imageMatcher = CQ_IMAGE.matcher(message);
        while (imageMatcher.find()) {
            list.add(parseImageParams(imageMatcher.group(1)));
        }
        Matcher fileMatcher = CQ_FILE.matcher(message);
        while (fileMatcher.find()) {
            CqImageSegment segment = parseFileParams(fileMatcher.group(1));
            if (isImageFileSegment(segment)) {
                list.add(segment);
            }
        }
        return list;
    }

    private static CqImageSegment parseImageParams(String paramPart) {
        CqImageSegment segment = new CqImageSegment();
        fillParams(segment, paramPart, false);
        return segment;
    }

    private static CqImageSegment parseFileParams(String paramPart) {
        CqImageSegment segment = new CqImageSegment();
        segment.setFileUpload(true);
        fillParams(segment, paramPart, true);
        return segment;
    }

    private static void fillParams(CqImageSegment segment, String paramPart, boolean includeName) {
        if (paramPart == null || paramPart.isEmpty()) {
            return;
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
                case "name":
                    if (includeName) {
                        segment.setName(value);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private static boolean isImageFileSegment(CqImageSegment segment) {
        return hasImagePath(segment.getName())
                || hasImagePath(segment.getFile())
                || hasImagePath(segment.getUrl());
    }

    /**
     * 路径/URL 是否指向可审核图片（按 basename 扩展名判断）。
     */
    public static boolean hasImagePath(String path) {
        return extractImageSuffix(path) != null;
    }

    /**
     * 从路径或 URL 提取小写扩展名（含点），非图片类型返回 null。
     */
    public static String extractImageSuffix(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        String name = path;
        int query = name.indexOf('?');
        if (query >= 0) {
            name = name.substring(0, query);
        }
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return null;
        }
        String ext = name.substring(dot).toLowerCase(Locale.ROOT);
        return IMAGE_EXTENSIONS.contains(ext) ? ext : null;
    }

    /**
     * 单张图片 CQ 参数。
     * <p>
     * go-cqhttp 上报通常同时带 file 与 url；优先用 url 下载，否则通过 get_image / get_file 解析。
     */
    @lombok.Data
    public static class CqImageSegment {
        /** CQ 码中的 file 字段（缓存文件名或 URL） */
        private String file;
        /** 可下载地址，或 file:/// 本地路径 */
        private String url;
        /**
         * CQ:file 的 name 字段（原始文件名）
         */
        private String name;
        /**
         * 是否来自 [CQ:file]（下载时优先 get_file）
         */
        private boolean fileUpload;
    }
}
