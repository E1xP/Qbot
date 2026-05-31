package com.bot.groupModeration.plugin;

import com.bot.event.message.CQGroupMessageEvent;
import com.bot.groupModeration.config.GroupModerationConfig;
import com.bot.groupModeration.pojo.GroupModerationItem;
import com.bot.groupModeration.service.GroupModerationService;
import com.bot.groupModeration.util.CqImageParser;
import com.bot.robot.CQPlugin;
import com.bot.robot.CoolQ;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Optional;

/**
 * 群消息入口插件：在总开关与群配置启用时，将含可审核图片的消息交给 {@link GroupModerationService}。
 * <p>
 * 须排在 {@code FilterPlugin} 之后，且 Filter 对含 {@code [CQ:image]} / 图片类 {@code [CQ:file]} 的消息
 * 返回 {@code MESSAGE_IGNORE}，否则后续插件不会执行。
 */
@Component
public class GroupModerationPlugin extends CQPlugin {

    @Resource
    private GroupModerationConfig config;
    @Resource
    private GroupModerationService groupModerationService;

    /**
     * 含图且群已配置时异步入队；始终 {@code MESSAGE_IGNORE}，不阻断后续插件。
     */
    @Override
    public int onGroupMessage(CoolQ cq, CQGroupMessageEvent event) {
        if (!config.isEnable()) {
            return MESSAGE_IGNORE;
        }
        Optional<GroupModerationItem> groupOpt = config.findGroup(event.getGroupId());
        if (!groupOpt.isPresent() || !CqImageParser.hasImage(event.getMessage())) {
            return MESSAGE_IGNORE;
        }
        groupModerationService.enqueue(cq, event, groupOpt.get());
        return MESSAGE_IGNORE;
    }
}
