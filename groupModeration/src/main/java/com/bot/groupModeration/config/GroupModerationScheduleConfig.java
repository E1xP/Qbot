package com.bot.groupModeration.config;

import com.bot.groupModeration.service.ImageStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 群审定时任务：清理超保留期的本地违规图片。
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
