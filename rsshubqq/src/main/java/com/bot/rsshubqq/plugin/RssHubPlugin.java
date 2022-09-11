package com.bot.rsshubqq.plugin;

import net.lz1998.pbbot.bot.Bot;
import net.lz1998.pbbot.bot.BotPlugin;
import onebot.OnebotEvent;
import org.jetbrains.annotations.NotNull;
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
public class RssHubPlugin extends BotPlugin {

    @Override
    public int onGroupMessage(@NotNull Bot bot, @NotNull OnebotEvent.GroupMessageEvent event) {
        return super.onGroupMessage(bot, event);
    }

    @Override
    public int onPrivateMessage(@NotNull Bot bot, @NotNull OnebotEvent.PrivateMessageEvent event) {
        return super.onPrivateMessage(bot, event);
    }
}
