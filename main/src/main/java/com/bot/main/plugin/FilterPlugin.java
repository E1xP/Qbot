package com.bot.main.plugin;

import com.bot.event.message.CQGroupMessageEvent;
import com.bot.event.message.CQPrivateMessageEvent;
import com.bot.main.config.BotConfig;
import com.bot.robot.CQPlugin;
import com.bot.robot.CoolQ;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
            //开启信息回复
            return MESSAGE_IGNORE;
        } else {
            //关闭信息回复
            return MESSAGE_BLOCK;
        }
    }

    @Override
    public int onGroupMessage(CoolQ cq, CQGroupMessageEvent event) {
        if (replyGroupFlag && event.getMessage().trim().startsWith("./")) {
            //开启信息回复且为指令
            return MESSAGE_IGNORE;
        } else {
            //关闭消息回复或非指令
            return MESSAGE_BLOCK;
        }
    }
}
