package com.bot.rsshubqq.plugin;

import com.bot.event.message.CQGroupMessageEvent;
import com.bot.event.message.CQPrivateMessageEvent;
import com.bot.robot.CQPlugin;
import com.bot.robot.CoolQ;
import org.springframework.stereotype.Component;


/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.qbot.rsshubqq
 * @CLASS_NAME RssHubPlgin
 * @Description TODO
 * @Date 2022/2/18 下午 2:57
 **/
@Component
public class RssHubPlugin extends CQPlugin {

    @Override
    public int onGroupMessage(CoolQ cq, CQGroupMessageEvent event) {
        return super.onGroupMessage(cq, event);
    }

    @Override
    public int onPrivateMessage(CoolQ cq, CQPrivateMessageEvent event) {
        return super.onPrivateMessage(cq, event);
    }
}
