package com.bot.main.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.main.config
 * @CLASS_NAME RestTemplateConfig
 * @Description TODO
 * @Date 2022/5/26 026 上午 11:59
 **/
@Configuration
@ConfigurationProperties(prefix = "rest-template-config")
public class RestTemplateConfig {

    int connectionTimeout;

    int readTimeout;

    @Bean
    public RestTemplate restTemplate(){
        SimpleClientHttpRequestFactory clientHttpRequestFactory
                = new SimpleClientHttpRequestFactory();
        clientHttpRequestFactory.setConnectTimeout(connectionTimeout * 1000);
        clientHttpRequestFactory.setReadTimeout(readTimeout * 1000);
        return new RestTemplate(clientHttpRequestFactory);
    }
}
