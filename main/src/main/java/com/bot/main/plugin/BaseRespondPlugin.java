package com.bot.main.plugin;

import com.bot.main.config.BotConfig;
import com.bot.main.config.PingConfig;
import com.bot.main.service.BotService;
import com.bot.utils.CQCodeExtend;
import lombok.extern.slf4j.Slf4j;
import net.lz1998.cq.entity.CQUser;
import net.lz1998.cq.event.message.CQGroupMessageEvent;
import net.lz1998.cq.event.message.CQPrivateMessageEvent;
import net.lz1998.cq.event.request.CQGroupRequestEvent;
import net.lz1998.cq.retdata.GroupData;
import net.lz1998.cq.robot.CQPlugin;
import net.lz1998.cq.robot.CoolQ;
import net.lz1998.cq.utils.CQCode;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
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
public class BaseRespondPlugin extends CQPlugin {

    /**
     * Bot服务
     */
    @Resource
    BotService botService;

    /**
     * bot配置类
     */
    @Resource
    BotConfig botConfig;

    /**
     * Ping相应配置类
     */
    @Resource
    PingConfig pingConfig;

    /**
     * 最后发送时间列表
     */
    ArrayList<Long> lastFiveTime = new ArrayList<>();

    /**
     * 允许加入群列表
     */
    List<Long> allowJoinGroupList = new ArrayList<>();

    /**
     * 用于响应./help指令的内容
     */
    String helpMessage =
            "./ping （用于激活机器人响应\n" +
                    "./echo [复读内容] （用于使机器人复读，需admin\n" +
                    "./help （用于查看帮助内容\n" +
                    "./steam-help （用于查看Steam模块的帮助内容";

    @Override
    public int onGroupRequest(CoolQ cq, CQGroupRequestEvent event) {
        boolean approve = false;
        if (allowJoinGroupList.contains(event.getGroupId())) {
            approve = true;
            cq.sendPrivateMsg(botConfig.getAdmins().get(0), "已允许" + event.getGroupId() + "加群邀请", true);
        } else {
            cq.sendPrivateMsg(botConfig.getAdmins().get(0), "已拒绝" + event.getGroupId() + "加群邀请", true);
        }
        cq.setGroupAddRequest(event.getFlag(), event.getSubType(), approve, "");
        return MESSAGE_BLOCK;
    }

    /**
     * 接收到私聊消息
     *
     * @param cq    cqBot实体类
     * @param event 私聊消息事件
     * @return 消息状态
     */
    @Override
    public int onPrivateMessage(CoolQ cq, CQPrivateMessageEvent event) {
        CQUser sender = event.getSender();
        if (botConfig.getAdmins().contains(sender.getUserId())) {
            String comToken = event.getMessage().trim().toLowerCase(Locale.ROOT).split(" ")[0];
            switch (comToken) {
                case "./status"://发送QQBot状态
                    onStatusPrivateMessage(cq, event);
                    break;
                case "./ping"://发送ping消息
                    onPingPrivateMessage(cq, event);
                    break;
                case "./send"://发送群消息
                    onSendPrivateMessage(cq, event);
                    break;
                case "./joingroup"://加入某群
                    onJoinGroupPrivateMessage(cq, event);
                default:
                    onErrorCommandPrivateMessage(cq, event);
            }
            return MESSAGE_BLOCK;
        }
        return MESSAGE_IGNORE;
    }

    /**
     * 接收到群消息
     *
     * @param cq    cqBot实体类
     * @param event 群消息事件
     * @return 消息状态
     */
    @Override
    public int onGroupMessage(CoolQ cq, CQGroupMessageEvent event) {
        String comToken = event.getMessage().trim().toLowerCase(Locale.ROOT).split(" ")[0];
        if (event.getMessage().trim().toLowerCase(Locale.ROOT).startsWith("./")) {
            switch (comToken) {
                case "./ping":
                    onPingGroupMessage(cq, event);
                    break;
                case "./echo":
                    onEchoGroupMessage(cq, event);
                    break;
                case "./help":
                    onHelpGroupMessage(cq, event);
                    break;
                default:
                    onErrorCommandGroupMessage(cq, event);
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
     * @param cq    cqBot实体类
     * @param event 群消息事件
     */
    private void onPingGroupMessage(CoolQ cq, CQGroupMessageEvent event) {
        //若消息以./ping开头
        long currentTime = System.currentTimeMillis();
        //在 消息间隔*消息总数时间 内能最大能发送 消息间隔 条相应
        if (lastFiveTime.size() < pingConfig.getMessageCount() || lastFiveTime.get(0) + pingConfig.getMessageGap() * pingConfig.getMessageCount() < currentTime) {
            log.info("响应Ping: " + event);
            //压入最后一次发送时间
            lastFiveTime.add(currentTime);
            if (lastFiveTime.size() > pingConfig.getMessageCount())
                lastFiveTime.remove(0);
            String message = CQCodeExtend.reply(event.getMessageId()) + "Pong!";
            cq.sendGroupMsg(event.getGroupId(), message, false);
        } else {
            log.info("触发限速: " + event);
        }
    }

    /**
     * 接收到群-Echo消息
     *
     * @param cq    cqBot实体类
     * @param event 群消息事件
     */
    private void onEchoGroupMessage(CoolQ cq, CQGroupMessageEvent event) {
        CQUser sender = event.getSender();
        if (botConfig.getAdmins().contains(sender.getUserId())) {
            log.info("响应Echo" + event);
            String message = event.getMessage().substring(event.getMessage().indexOf(" "));
            cq.sendGroupMsg(event.getGroupId(), message, false);
        }
    }

    /**
     * 接收到群-不匹配的指令
     *
     * @param cq    cqBot实体类
     * @param event 群消息事件
     */
    private void onErrorCommandGroupMessage(CoolQ cq, CQGroupMessageEvent event) {
        CQUser sender = event.getSender();
        String message = CQCode.at(sender.getUserId()) + "指令错误：" + event.getMessage().split(" ")[0];
        cq.sendGroupMsg(event.getGroupId(), message, false);
    }

    /**
     * 接收到群-帮助指令
     *
     * @param cq    cqBot实体类
     * @param event 群消息事件
     */
    private void onHelpGroupMessage(CoolQ cq, CQGroupMessageEvent event) {
        cq.sendGroupMsg(event.getGroupId(), helpMessage, false);
    }

    /*收到私聊消息*/

    /**
     * 收到到私聊-加入群
     *
     * @param cq    cqBot实体类
     * @param event 群消息事件
     */
    private void onJoinGroupPrivateMessage(CoolQ cq, CQPrivateMessageEvent event) {
        CQUser sender = event.getSender();
        if (botConfig.getAdmins().contains(sender.getUserId())) {//判定是否为管理员
            Long groupId = null;
            try {
                groupId = Long.valueOf(event.getMessage().split(" ")[1]);
            } catch (NumberFormatException e) {
                cq.sendPrivateMsg(event.getUserId(), "参数错误-非群号", true);
            }
            allowJoinGroupList.add(groupId);
        }
    }

    /**
     * 接收到私聊-不匹配的指令
     *
     * @param cq    cqBot实体类
     * @param event 私聊消息事件
     */
    private void onErrorCommandPrivateMessage(CoolQ cq, CQPrivateMessageEvent event) {
        CQUser sender = event.getSender();
        String message = "错误指令：" + event.getMessage().split(" ")[0];
        cq.sendPrivateMsg(sender.getUserId(), message, false);
    }

    /**
     * 接收到私聊-Ping指令
     *
     * @param cq    cqBot实体类
     * @param event 私聊消息事件
     */
    private void onPingPrivateMessage(CoolQ cq, CQPrivateMessageEvent event) {
        CQUser sender = event.getSender();
        log.info("响应Ping: " + event);
        String message = "Pong!";
        cq.sendPrivateMsg(sender.getUserId(), message, false);
    }

    /**
     * 接受到私聊-获取当前BOT状态
     *
     * @param cq    cqBot实体类
     * @param event 私聊消息事件
     */
    private void onStatusPrivateMessage(CoolQ cq, CQPrivateMessageEvent event) {
        CQUser sender = event.getSender();
        log.info("响应状态：" + event);
        String message = botService.getBotStatus(cq);
        cq.sendPrivateMsg(sender.getUserId(), message, false);
    }

    /**
     * 接收到私聊-向群发送消息
     *
     * @param cq    cqBot实体类
     * @param event 私聊消息事件
     */
    private void onSendPrivateMessage(CoolQ cq, CQPrivateMessageEvent event) {
        CQUser sender = event.getSender();
        if (botConfig.getAdmins().contains(sender.getUserId())) {//判定是否为管理员
            log.info("发送群消息" + event);
            if (event.getMessage().split(" ").length < 3) {
                String message = "发送群消息指令格式错误：./send 群号 内容";
                cq.sendPrivateMsg(sender.getUserId(), message, false);
            } else {
                String groupStr = event.getMessage().split(" ")[1];
                try {
                    long groupId = Long.parseLong(groupStr);
                    String sendMessage = event.getMessage().split(" [0-9]+ ")[1];
                    if (cq.getGroupList().getData().stream().map(GroupData::getGroupId).collect(Collectors.toList()).contains(groupId)) {
                        //若已添加该群
                        cq.sendGroupMsg(groupId, sendMessage, false);
                    } else {
                        //未加群
                        cq.sendPrivateMsg(sender.getUserId(), "未加入该群:" + groupStr, false);
                    }
                } catch (NumberFormatException exception) {
                    cq.sendPrivateMsg(sender.getUserId(), "输入群号非数字", false);
                } catch (IndexOutOfBoundsException exception) {
                    cq.sendPrivateMsg(sender.getUserId(),"找不到发送信息内容",false);
                }
            }
        }
    }
}
