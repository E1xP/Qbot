package com.bot.main.plugin;

import com.bot.event.message.CQGroupMessageEvent;
import com.bot.event.message.CQPrivateMessageEvent;
import com.bot.groupModeration.util.CqImageParser;
import com.bot.main.config.BotConfig;
import com.bot.robot.CQPlugin;
import com.bot.robot.CoolQ;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;


/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.e1xp.qbot_project.plugin
 * @CLASS_NAME MainPlugin
 * @Description TODO
 * @Date 2022/2/18 下午 1:31
 **/
@Component
public class FilterPlugin extends CQPlugin {

    @Value("${bot.replyGroup}")
    boolean replyGroupFlag;
    @Value("${bot.replyPrivate}")
    boolean replyPrivateFlag;

    @Resource
    BotConfig botConfig;

    @Override
    public int onPrivateMessage(CoolQ cq, CQPrivateMessageEvent event) {
        if (replyPrivateFlag) {
            return MESSAGE_IGNORE;
        }
        return MESSAGE_BLOCK;
    }

    @Override
    public int onGroupMessage(CoolQ cq, CQGroupMessageEvent event) {
        String message = event.getMessage();
        if (replyGroupFlag && message != null && message.trim().startsWith("./")) {
            return MESSAGE_IGNORE;
        }
        if (CqImageParser.hasImage(message)) {
            return MESSAGE_IGNORE;
        }
        return MESSAGE_BLOCK;
    }
}
