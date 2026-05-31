package com.bot.groupModeration.config;

import com.bot.groupModeration.service.ImageStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 群审定时任务：按 {@code storage-retention-days} 清理本地违规图目录与 {@code _temp} 临时文件。
 * <p>
 * Cron 由 {@code group-moderation.storage-cleanup-cron} 配置，默认每天 03:00。
 */
@Component
@Slf4j
public class GroupModerationScheduleConfig {

    @Resource
    private GroupModerationConfig config;
    @Resource
    private ImageStorageService imageStorageService;

    @Scheduled(cron = "${group-moderation.storage-cleanup-cron:0 0 3 * * ?}")
    public void cleanupStorage() {
        if (config.getStorageRetentionDays() <= 0) {
            return;
        }
        try {
            imageStorageService.cleanupExpired();
        } catch (Exception e) {
            log.error("群审存储定时清理失败", e);
        }
    }
}
