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
}
