package com.bot.main.plugin;

import com.bot.main.config.BotConfig;
import com.bot.main.config.PingConfig;
import com.bot.main.service.BotService;
import lombok.extern.slf4j.Slf4j;
import net.lz1998.cq.entity.CQUser;
import net.lz1998.cq.event.message.CQGroupMessageEvent;
import net.lz1998.cq.event.message.CQPrivateMessageEvent;
import net.lz1998.cq.robot.CQPlugin;
import net.lz1998.cq.robot.CoolQ;
import net.lz1998.cq.utils.CQCode;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Locale;

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

    @Resource
    BotService botService;//Bot服务

    @Resource
    BotConfig botConfig;//bot配置类

    @Resource
    PingConfig pingConfig;//Ping相应配置类

    ArrayList<Long> lastFiveTime=new ArrayList<>();//最后发送时间列表

    /**
     * 接收到私聊消息
     * @param cq cqBot实体类
     * @param event 私聊消息事件
     * @return 消息状态
     */
    @Override
    public int onPrivateMessage(CoolQ cq, CQPrivateMessageEvent event) {
        CQUser sender = event.getSender();
        if(botConfig.getAdmins().contains(sender.getUserId())){
            String comToken=event.getMessage().trim().toLowerCase(Locale.ROOT).split(" ")[0];
            switch (comToken){
                case "./status":
                    onStatusPrivateMessage(cq,event);
                    break;
                case "./ping":
                    onPingPrivateMessage(cq,event);
                    break;
                default:
                    onErrorCommandPrivateMessage(cq,event);
                    break;
            }
            return MESSAGE_BLOCK;
        }
        return MESSAGE_IGNORE;
    }

    /**
     * 接收到群消息
     * @param cq cqBot实体类
     * @param event 群消息事件
     * @return 消息状态
     */
    @Override
    public int onGroupMessage(CoolQ cq, CQGroupMessageEvent event) {
        String comToken=event.getMessage().trim().toLowerCase(Locale.ROOT).split(" ")[0];
        if(event.getMessage().trim().toLowerCase(Locale.ROOT).startsWith("./")) {
            switch (comToken){
                case "./ping":
                    onPingGroupMessage(cq,event);
                    break;
                case "./echo":
                    onEchoGroupMessage(cq,event);
                    break;
                default:
                    onErrorCommandGroupMessage(cq,event);
                    break;
            }
            return MESSAGE_BLOCK;
        }
        return MESSAGE_IGNORE;
    }

    /**
     * 当接收到群内Ping消息
     * 消息间隔*消息总数时间 内能最大能发送 消息间隔 条响应
     * @param cq cqBot实体类
     * @param event 群消息事件
     */
    private void onPingGroupMessage(CoolQ cq, CQGroupMessageEvent event){
        //若消息以./ping开头
        long currentTime=System.currentTimeMillis();
        //在 消息间隔*消息总数时间 内能最大能发送 消息间隔 条相应
        if(lastFiveTime.size()<pingConfig.getMessageCount()||lastFiveTime.get(0)+pingConfig.getMessageGap()*pingConfig.getMessageCount()<currentTime) {
            log.info("响应Ping: "+event);
            //压入最后一次发送时间
            lastFiveTime.add(currentTime);
            if(lastFiveTime.size()>pingConfig.getMessageCount())
                lastFiveTime.remove(0);
            String message = CQCode.at(event.getUserId()) + "Pong!";
            cq.sendGroupMsg(event.getGroupId(), message, false);
        }else{
            log.info("触发限速: "+ event);
        }
    }

    /**
     * 当接收到群内Echo消息
     * @param cq cqBot实体类
     * @param event 群消息事件
     */
    private void onEchoGroupMessage(CoolQ cq,CQGroupMessageEvent event) {
        CQUser sender = event.getSender();
        if (botConfig.getAdmins().contains(sender.getUserId())) {
            log.info("响应Echo" + event);
            String message = event.getMessage().substring(event.getMessage().indexOf(" "));
            cq.sendGroupMsg(event.getGroupId(), message, false);
        }
    }

    /**
     * 当接收到群内不匹配的指令
     * @param cq cqBot实体类
     * @param event 群消息事件
     */
    private void onErrorCommandGroupMessage(CoolQ cq,CQGroupMessageEvent event){
        CQUser sender = event.getSender();
        String message = CQCode.at(sender.getUserId()) +"指令错误："+ event.getMessage().split(" ")[0];
        cq.sendGroupMsg(event.getGroupId(), message, false);
    }

    /**
     * 当接收到私聊不匹配的指令
     * @param cq cqBot实体类
     * @param event 私聊消息事件
     */
    private void onErrorCommandPrivateMessage(CoolQ cq,CQPrivateMessageEvent event){
        CQUser sender = event.getSender();
        String message="错误指令："+event.getMessage().split(" ")[0];
        cq.sendPrivateMsg(sender.getUserId(),message,false);
    }

    /**
     * 当接收到私聊的Ping指令
     * @param cq cqBot实体类
     * @param event 私聊消息事件
     */
    private void onPingPrivateMessage(CoolQ cq, CQPrivateMessageEvent event){
        CQUser sender=event.getSender();
        log.info("响应Ping: "+event);
        String message="Pong!";
        cq.sendPrivateMsg(sender.getUserId(),message,false);
    }

    /**
     * 当接收到私聊status消息
     * @param cq cqBot实体类
     * @param event 私聊消息事件
     */
    private void onStatusPrivateMessage(CoolQ cq,CQPrivateMessageEvent event){
        CQUser sender=event.getSender();
        log.info("响应状态："+event);
        String message=botService.getBotStatus(cq);
        cq.sendPrivateMsg(sender.getUserId(),message,false);
    }
}
