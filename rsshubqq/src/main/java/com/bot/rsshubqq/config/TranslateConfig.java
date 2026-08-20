package com.bot.rsshubqq.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.rsshubqq.config
 * @CLASS_NAME translateConfig
 * @Description 翻译配置；按 apiList 顺序尝试，失败则用下一个
 * @Date 2022/2/20 上午 11:59
 **/
@Data
@Configuration
@ConfigurationProperties(prefix = "translate")
public class TranslateConfig {
    /**
     * 翻译的目标语言
     */
    String targetLanguage;

    /**
     * 翻译接口列表（按顺序尝试）
     */
    List<Api> apiList = new ArrayList<>();

    @Data
    public static class Api {
        /**
         * 使用什么接口：baidu / deepl-serverless
         */
        TranslateApiName apiName;
        /**
         * 翻译app应用id（百度）
         */
        String appId;
        /**
         * 翻译app对应的密钥（百度）
         */
        String securityKey;
        /**
         * 翻译接口url
         */
        String url;
    }
}
