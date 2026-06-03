package com.bot.groupModeration.service;

import com.bot.groupModeration.config.GroupModerationConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * 违规图片落盘：{@code {storage-base-path}/{groupId}/}；临时目录 {@code _temp} 供「只通知不持久化」场景。
 */
@Service
@Slf4j
public class ImageStorageService {

    @Resource
    private GroupModerationConfig config;

    /**
     * 持久化保存：{@code {storage-base-path}/{groupId}/{timestamp}_{userId}_{messageId}.ext}
     */
    public File save(long groupId, long userId, int messageId, byte[] imageBytes,
                     String fileHint, String urlHint) throws IOException {
        String suffix = ImageFetchService.guessSuffix(imageBytes, fileHint, urlHint);
        File dir = new File(config.getStorageBasePath(), String.valueOf(groupId));
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

    private int deleteExpiredUnder(File dir, long cutoffMs) {
        if (!dir.isDirectory()) {
            return 0;
        }
        File[] children = dir.listFiles();
        if (children == null) {
            return 0;
        }
        int count = 0;
        for (File child : children) {
            if (child.isFile()) {
                if (child.lastModified() < cutoffMs && child.delete()) {
                    count++;
                }
            } else if (child.isDirectory()) {
                count += deleteExpiredUnder(child, cutoffMs);
                deleteIfEmpty(child);
            }
        }
        return count;
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
     * 删除超过保留天数的持久化文件（{@code {groupId}/} 下，含历史日期子目录）及 {@code _temp} 过期文件。
     *
     * @return [删除的持久化文件数, 删除的临时文件数]
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

        long cutoffMs = System.currentTimeMillis() - retentionDays * 86_400_000L;
        int deletedPersistedFiles = 0;
        int deletedTempFiles = 0;

        File[] children = base.listFiles();
        if (children == null) {
            return new int[]{0, 0};
        }
        for (File child : children) {
            if ("_temp".equals(child.getName())) {
                deletedTempFiles += deleteTempFilesOlderThan(child, cutoffMs);
                continue;
            }
            if (!child.isDirectory()) {
                continue;
            }
            deletedPersistedFiles += deleteExpiredUnder(child, cutoffMs);
            deleteIfEmpty(child);
        }
        if (deletedPersistedFiles > 0 || deletedTempFiles > 0) {
            log.info("群审存储清理 retentionDays={} deletedPersistedFiles={} deletedTempFiles={} base={}",
                    retentionDays, deletedPersistedFiles, deletedTempFiles, base.getAbsolutePath());
        }
        return new int[]{deletedPersistedFiles, deletedTempFiles};
    }
}
