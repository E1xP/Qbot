package com.bot.main.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.main.config
 * @CLASS_NAME EmailConfig
 * @Description TODO
 * @Date 2023/11/5 0005 下午 11:22
 **/
@Data
@Configuration
@ConfigurationProperties(prefix = "email")
public class EmailConfig {
    String username;    //用户名
    String password;    //密码
    String smtpHost;    //SMTP地址
    String smtpPort;    //SMTP端口
    String smtpAuth;    //是否需要认证
    String smtpTls;     //是否启用TLS
}
