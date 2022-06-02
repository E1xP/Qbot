package com.bot.main.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
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
}
