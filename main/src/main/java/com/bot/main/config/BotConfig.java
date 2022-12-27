package com.bot.main.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.main.config
 * @CLASS_NAME BotConfig
 * @Description TODO
 * @Date 2022/2/19 下午 3:30
 **/
@Data
@Configuration
@ConfigurationProperties(prefix = "bot")
public class BotConfig {
    boolean replyGroup;     //是否响应群消息
    boolean replyPrivate;   //是否响应私聊消息
    List<Long> admins;      //管理员列表
    boolean earlyWarningGroupEnable = false;    //告警信息是否发送给群聊
    boolean earlyWarningPrivateEnable = false;  //告警信息是否发送给私聊
    List<Long> earlyWarningGroupList = new ArrayList<>();   //告警信息发送群聊列表
    List<Long> earlyWarningPrivateList = new ArrayList<>(); //告警信息发送私聊列表
}
