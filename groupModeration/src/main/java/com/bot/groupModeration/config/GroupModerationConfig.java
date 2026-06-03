package com.bot.groupModeration.config;

import com.bot.groupModeration.pojo.GroupModerationItem;
import com.bot.groupModeration.pojo.GroupNotifyConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 群审模块总配置，绑定 {@code application-groupModeration.yml} 中 {@code group-moderation} 前缀。
 * <p>
 * {@link #findGroup(long)} 仅返回 {@code groups} 中 {@code enable=true} 且群号匹配的项。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "group-moderation")
public class GroupModerationConfig {

    /**
     * 模块总开关
     */
    private boolean enable;

    /**
     * 内置模型选择：224（MobileNet，快）或 299（Inception，稍慢）。
     * 也支持 mobilenet224 / inception299 等别名，见 {@link OnnxModelVariant#from(String)}。
     */
    private String modelVariant = "224";

    /**
     * 可选：外部 ONNX 绝对/相对路径。非空且文件存在时优先于内置 classpath 模型。
     */
    private String modelPath;

    /**
     * 模型输入边长；为 0 或未配置时按 model-variant 自动取 224 或 299。
     */
    private int modelInputSize;

    /**
     * ONNX Runtime 单会话 intra-op 线程数；队列已是单线程顺序推理，建议保持 1。
     */
    private int onnxThreads = 1;

    /**
     * 初筛任务队列容量；满时丢弃。
     */
    private int taskQueueCapacity = 256;

    /**
     * 精判任务队列容量；满时丢弃（初筛已过线但未精判的消息）。
     */
    private int refineQueueCapacity = 256;

    /**
     * 违规图片存储根目录
     */
    private String storageBasePath = "./data/moderation/storage";

    /**
     * 本地违规图保留天数；超过后由定时任务按文件最后修改时间删除各群目录下过期文件。0 表示不自动清理。
     */
    private int storageRetentionDays = 7;

    /**
     * 存储清理 cron，默认每天 03:00
     */
    private String storageCleanupCron = "0 0 3 * * ?";

    /**
     * 全局告警默认配置
     */
    private GroupNotifyConfig notify = new GroupNotifyConfig();

    /**
     * NudeNet 精判模型配置
     */
    private NudeNetConfig nudenet = new NudeNetConfig();

    /**
     * 初筛置信度阈值（百分比，默认 60）。≥ 此值进入 NudeNet 精判。
     */
    private double prescreenThreshold = 60d;

    /**
     * 审核结果缓存最大条数（LRU 淘汰）。
     */
    private int resultCacheMaxSize = 1024;

    /**
     * 审核结果缓存：最后访问后过期时间（分钟）。
     */
    private long resultCacheExpireAfterAccessMinutes = 1440;

    /**
     * 各群独立配置列表
     */
    private List<GroupModerationItem> groups = new ArrayList<>();

    /**
     * 查找已启用的群配置。
     *
     * @param groupId 群号
     * @return 命中且 enable=true 的配置项
     */
    public Optional<GroupModerationItem> findGroup(long groupId) {
        if (groups == null) {
            return Optional.empty();
        }
        return groups.stream()
                .filter(g -> g.getGroupId() == groupId && g.isEnable())
                .findFirst();
    }

    /**
     * NudeNet 精判配置（绑定 {@code group-moderation.nudenet}）。
     */
    @Data
    public static class NudeNetConfig {
        private boolean enable = true;
        private String modelPath;
        private int inputSize = 640;
        private float nmsIouThreshold = 0.45f;
        private float minDetectionScore = 0.01f;
    }
}
