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
 * 群审模块总配置，绑定 application-groupModeration.yml 中 group-moderation 前缀。
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
     * 待审核任务队列容量；入队非阻塞，满时丢弃并打日志。
     */
    private int taskQueueCapacity = 256;

    /**
     * 违规图片存储根目录
     */
    private String storageBasePath = "./data/moderation/storage";

    /**
     * 全局告警默认配置
     */
    private GroupNotifyConfig notify = new GroupNotifyConfig();

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
}
