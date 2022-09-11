package com.bot.main.plugin;

import com.bot.main.config.BotConfig;
import com.bot.main.config.PingConfig;
import com.bot.main.service.BotService;
import lombok.extern.slf4j.Slf4j;
import net.lz1998.pbbot.bot.Bot;
import net.lz1998.pbbot.bot.BotPlugin;
import net.lz1998.pbbot.utils.Msg;
import onebot.OnebotApi;
import onebot.OnebotEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.qbot.main.plugin
 * @CLASS_NAME PingPlugin
 * @Description TODO
 * @Date 2022/2/18 下午 3:11
 **/
@Component
@Slf4j
public class BaseRespondPlugin extends BotPlugin {

    @Resource
    BotService botService;//Bot服务

    @Resource
    BotConfig botConfig;//bot配置类

    @Resource
    PingConfig pingConfig;//Ping相应配置类

    ArrayList<Long> lastFiveTime=new ArrayList<>();//最后发送时间列表

    /**
     * 接收到私聊消息
     *
     * @param bot   botBot实体类
     * @param event 私聊消息事件
     * @return 消息状态
     */
    @Override
    public int onPrivateMessage(@NotNull Bot bot, OnebotEvent.PrivateMessageEvent event) {
        if (botConfig.getAdmins().contains(event.getUserId())) {
            String comToken = event.getRawMessage().trim().toLowerCase(Locale.ROOT).split(" ")[0];
            switch (comToken) {
                case "./status"://发送QQBot状态
                    onStatusPrivateMessage(bot, event);
                    break;
                case "./ping"://发送ping消息
                    onPingPrivateMessage(bot, event);
                    break;
                case "./send"://发送群消息
                    onSendPrivateMessage(bot, event);
                    break;
                default:
                    onErrorCommandPrivateMessage(bot, event);
            }
            return MESSAGE_BLOCK;
        }
        return MESSAGE_IGNORE;
    }

    /**
     * 接收到群消息
     *
     * @param bot   botBot实体类
     * @param event 群消息事件
     * @return 消息状态
     */
    @Override
    public int onGroupMessage(@NotNull Bot bot, OnebotEvent.GroupMessageEvent event) {
        String comToken = event.getRawMessage().trim().toLowerCase(Locale.ROOT).split(" ")[0];
        if (event.getRawMessage().trim().toLowerCase(Locale.ROOT).startsWith("./")) {
            switch (comToken) {
                case "./ping":
                    onPingGroupMessage(bot, event);
                    break;
                case "./echo":
                    onEchoGroupMessage(bot, event);
                    break;
                default:
                    onErrorCommandGroupMessage(bot, event);
                    break;
            }
            return MESSAGE_BLOCK;
        }
        return MESSAGE_IGNORE;
    }

    /*收到群聊消息*/

    /**
     * 接收到群内-Ping消息
     * 消息间隔*消息总数时间 内能最大能发送 消息间隔 条响应
     *
     * @param bot   botBot实体类
     * @param event 群消息事件
     */
    private void onPingGroupMessage(Bot bot, OnebotEvent.GroupMessageEvent event) {
        //若消息以./ping开头
        long currentTime = System.currentTimeMillis();
        //在 消息间隔*消息总数时间 内能最大能发送 消息间隔 条相应
        if (lastFiveTime.size() < pingConfig.getMessageCount() || lastFiveTime.get(0) + pingConfig.getMessageGap() * pingConfig.getMessageCount() < currentTime) {
            log.info("响应Ping: " + event);
            //压入最后一次发送时间
            lastFiveTime.add(currentTime);
            if (lastFiveTime.size() > pingConfig.getMessageCount())
                lastFiveTime.remove(0);
            Msg msg = Msg.builder().reply(event.getMessageId()).text("Pong!");
            bot.sendGroupMsg(event.getGroupId(), msg, false);
        } else {
            log.info("触发限速: " + event);
        }
    }

    /**
     * 接收到群-Echo消息
     *
     * @param bot   botBot实体类
     * @param event 群消息事件
     */
    private void onEchoGroupMessage(Bot bot, OnebotEvent.GroupMessageEvent event) {
        if (botConfig.getAdmins().contains(event.getUserId())) {
            log.info("响应Echo" + event);
            Msg msg = Msg.builder().text(event.getRawMessage().substring(event.getRawMessage().indexOf(" ")));
            bot.sendGroupMsg(event.getGroupId(), msg, false);
        }
    }

    /**
     * 接收到群-不匹配的指令
     *
     * @param bot   botBot实体类
     * @param event 群消息事件
     */
    private void onErrorCommandGroupMessage(Bot bot, OnebotEvent.GroupMessageEvent event) {
        Msg msg = Msg.builder().reply(event.getMessageId()).text("指令错误：").text(event.getRawMessage().split(" ")[0]);
        bot.sendGroupMsg(event.getGroupId(), msg, false);
    }

    /*收到私聊消息*/

    /**
     * 接收到私聊-不匹配的指令
     *
     * @param bot   botBot实体类
     * @param event 私聊消息事件
     */
    private void onErrorCommandPrivateMessage(Bot bot, OnebotEvent.PrivateMessageEvent event) {
        String message = "错误指令：" + event.getRawMessage().split(" ")[0];
        bot.sendPrivateMsg(event.getUserId(), message, false);
    }

    /**
     * 接收到私聊-Ping指令
     *
     * @param bot   botBot实体类
     * @param event 私聊消息事件
     */
    private void onPingPrivateMessage(Bot bot, OnebotEvent.PrivateMessageEvent event) {
        log.info("响应Ping: " + event);
        String message = "Pong!";
        bot.sendPrivateMsg(event.getUserId(), message, false);
    }

    /**
     * 接受到私聊-获取当前BOT状态
     *
     * @param bot   botBot实体类
     * @param event 私聊消息事件
     */
    private void onStatusPrivateMessage(Bot bot, OnebotEvent.PrivateMessageEvent event) {
        log.info("响应状态：" + event);
        String message = botService.getBotStatus(bot);
        bot.sendPrivateMsg(event.getUserId(), message, false);
    }

    /**
     * 接收到私聊-向群发送消息
     *
     * @param bot   botBot实体类
     * @param event 私聊消息事件
     */
    private void onSendPrivateMessage(Bot bot, OnebotEvent.PrivateMessageEvent event) {
        if (botConfig.getAdmins().contains(event.getUserId())) {//判定是否为管理员
            log.info("发送群消息" + event);
            if (event.getRawMessage().split(" ").length < 3) {
                String message = "发送群消息指令格式错误：./send 群号 内容";
                bot.sendPrivateMsg(event.getUserId(), message, false);
            } else {
                String groupStr = event.getRawMessage().split(" ")[1];
                try {
                    long groupId = Long.parseLong(groupStr);
                    String sendMessage = event.getRawMessage().split(" [0-9]+ ")[1];
                    if (bot.getGroupList().getGroupList().stream().map(OnebotApi.GetGroupListResp.Group::getGroupId).collect(Collectors.toList()).contains(groupId)) {
                        //若已添加该群
                        bot.sendGroupMsg(groupId, sendMessage, false);
                    } else {
                        //未加群
                        bot.sendPrivateMsg(event.getUserId(), "未加入该群:" + groupStr, false);
                    }
                } catch (NumberFormatException exception) {
                    bot.sendPrivateMsg(event.getUserId(), "输入群号非数字", false);
                } catch (IndexOutOfBoundsException exception) {
                    bot.sendPrivateMsg(event.getUserId(), "找不到发送信息内容", false);
                }
            }
        }
    }
}
