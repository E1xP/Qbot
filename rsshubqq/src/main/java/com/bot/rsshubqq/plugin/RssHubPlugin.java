package com.bot.rsshubqq.plugin;

import net.lz1998.cq.event.message.CQGroupMessageEvent;
import net.lz1998.cq.event.message.CQPrivateMessageEvent;
import net.lz1998.cq.robot.CQPlugin;
import net.lz1998.cq.robot.CoolQ;
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
