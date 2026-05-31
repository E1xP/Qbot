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

@Component
public class GroupModerationPlugin extends CQPlugin {

    @Resource
    private GroupModerationConfig config;
    @Resource
    private GroupModerationService groupModerationService;

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
