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

    @Override
    public int onPrivateMessage(CoolQ cq, CQPrivateMessageEvent event) {
        CQUser sender = event.getSender();
        if(botConfig.getAdmins().contains(sender.getUserId())){
            String message;
            String comToken=event.getMessage().trim().toLowerCase(Locale.ROOT).split(" ")[0];
            switch (comToken){
                case "./status":
                    log.info("获取Bot状态: "+event);
                    message=botService.getBotStatus(cq);
                    break;
                case "./ping":
                    log.info("响应Ping: "+event);
                    message="Pong~";
                    break;
                default:
                    log.info("错误指令："+event);
                    message="错误指令："+comToken;
            }
            cq.sendPrivateMsg(event.getUserId(),message,false);
            return MESSAGE_BLOCK;
        }
        return MESSAGE_IGNORE;
    }

    /**
     * Ping应答
     * 在 消息间隔*消息总数时间 内能最大能发送 消息间隔 条响应
     * @param cq cqbot
     * @param event 获得事件
     * @return 消息状态
     */
    @Override
    public int onGroupMessage(CoolQ cq, CQGroupMessageEvent event) {
        if(event.getMessage().trim().toLowerCase(Locale.ROOT).contains("./ping")) {
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
            return MESSAGE_BLOCK;
        }
        return MESSAGE_IGNORE;
    }
}
