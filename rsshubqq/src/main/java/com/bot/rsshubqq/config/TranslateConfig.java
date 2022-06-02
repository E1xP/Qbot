package com.bot.rsshubqq.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.rsshubqq.config
 * @CLASS_NAME translateConfig
 * @Description TODO
 * @Date 2022/2/20 上午 11:59
 **/
@Data
@Configuration
@ConfigurationProperties(prefix = "translate")
public class TranslateConfig {
    /** 翻译app应用id */
    String appId;
    /** 翻译app对应的密钥 */
    String securityKey;
    /**翻译接口url*/
    String url;
    /**翻译的目标语言*/
    String targetLanguage;
}
