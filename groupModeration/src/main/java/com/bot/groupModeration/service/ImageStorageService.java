package com.bot.groupModeration.service;

import com.bot.groupModeration.config.GroupModerationConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 违规图片落盘：按群号、日期分子目录；临时目录供「只通知不持久化」场景使用。
 */
@Service
@Slf4j
public class ImageStorageService {

    private static final SimpleDateFormat DIR_FORMAT = new SimpleDateFormat("yyyyMMdd");

    @Resource
    private GroupModerationConfig config;

    /**
     * 持久化保存：{storage-base-path}/{groupId}/{yyyyMMdd}/{timestamp}_{userId}_{messageId}.ext
     *
     * @param groupId    来源群
     * @param userId     发送者 QQ
     * @param messageId  消息 ID
     * @param imageBytes 图片内容
     * @return 写入后的文件
     */
    public File save(long groupId, long userId, int messageId, byte[] imageBytes,
                     String fileHint, String urlHint) throws IOException {
        String suffix = ImageFetchService.guessSuffix(imageBytes, fileHint, urlHint);
        String day = DIR_FORMAT.format(new Date());
        File dir = new File(config.getStorageBasePath(), groupId + File.separator + day);
        if (!dir.exists() && !dir.mkdirs()) {
            log.warn("创建存储目录失败: {}", dir.getAbsolutePath());
        }
        String name = System.currentTimeMillis() + "_" + userId + "_" + messageId + suffix;
        File target = new File(dir, name);
        Files.write(target.toPath(), imageBytes);
        return target;
    }

    /**
     * 写入临时文件，用于未开启 save-enable 但仍需向告警群上传图片的场景；调用方负责删除。
     */
    public File saveToTemp(byte[] imageBytes, String fileHint, String urlHint) throws IOException {
        String suffix = ImageFetchService.guessSuffix(imageBytes, fileHint, urlHint);
        File dir = new File(config.getStorageBasePath(), "_temp");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File target = File.createTempFile("notify_", suffix, dir);
        Files.write(target.toPath(), imageBytes);
        return target;
    }

    private static int deleteTempFilesOlderThan(File tempDir, long cutoffMs) {
        if (!tempDir.isDirectory()) {
            return 0;
        }
        File[] files = tempDir.listFiles();
        if (files == null) {
            return 0;
        }
        int count = 0;
        for (File file : files) {
            if (file.isFile() && file.lastModified() < cutoffMs && file.delete()) {
                count++;
            }
        }
        return count;
    }

    private static int deleteRecursive(File file) {
        if (file == null || !file.exists()) {
            return 0;
        }
        int count = 0;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    count += deleteRecursive(child);
                }
            }
        }
        if (file.delete()) {
            count++;
        }
        return count;
    }

    private static void deleteIfEmpty(File dir) {
        if (!dir.isDirectory()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files != null && files.length == 0) {
            dir.delete();
        }
    }

    /**
     * 删除超过保留天数的持久化目录（{groupId}/{yyyyMMdd}）及 _temp 下过期文件。
     *
     * @return [删除的日期目录数, 删除的临时文件数]
     */
    public int[] cleanupExpired() {
        int retentionDays = config.getStorageRetentionDays();
        if (retentionDays <= 0) {
            return new int[]{0, 0};
        }
        File base = new File(config.getStorageBasePath());
        if (!base.isDirectory()) {
            return new int[]{0, 0};
        }

        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.format.DateTimeFormatter dayFmt = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd");
        long tempCutoffMs = System.currentTimeMillis() - retentionDays * 86_400_000L;

        int deletedDayDirs = 0;
        int deletedTempFiles = 0;

        File[] children = base.listFiles();
        if (children == null) {
            return new int[]{0, 0};
        }
        for (File child : children) {
            if ("_temp".equals(child.getName())) {
                deletedTempFiles += deleteTempFilesOlderThan(child, tempCutoffMs);
                continue;
            }
            if (!child.isDirectory()) {
                continue;
            }
            File[] dayDirs = child.listFiles();
            if (dayDirs == null) {
                continue;
            }
            for (File dayDir : dayDirs) {
                if (!dayDir.isDirectory()) {
                    continue;
                }
                try {
                    java.time.LocalDate dirDate = java.time.LocalDate.parse(dayDir.getName(), dayFmt);
                    long ageDays = java.time.temporal.ChronoUnit.DAYS.between(dirDate, today);
                    if (ageDays >= retentionDays) {
                        deletedDayDirs += deleteRecursive(dayDir) > 0 ? 1 : 0;
                    }
                } catch (Exception ignored) {
                    // 非 yyyyMMdd 目录名，跳过
                }
            }
            deleteIfEmpty(child);
        }
        if (deletedDayDirs > 0 || deletedTempFiles > 0) {
            log.info("群审存储清理 retentionDays={} deletedDayDirs={} deletedTempFiles={} base={}",
                    retentionDays, deletedDayDirs, deletedTempFiles, base.getAbsolutePath());
        }
        return new int[]{deletedDayDirs, deletedTempFiles};
    }
}
