package com.bot.main.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.main.config
 * @CLASS_NAME PingConfig
 * @Description TODO Ping响应配置类
 * @Date 2022/2/19 下午 5:07
 **/
@Data
@Configuration
@ConfigurationProperties(prefix = "ping-config")
public class PingConfig {
    int messageCount; //消息记录时间数
    long messageGap; //消息间隔（ms
}
