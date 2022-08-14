package com.bot.rsshubqq.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;
import java.util.Arrays;

/**
 * @author E1xP@foxmail.com 
 * @version 1.0
 * @PACKAGE_NAME com.bot.rsshubqq.config
 * @CLASS_NAME MvcConfig
 * @Description TODO 
 * @Date 2022/8/14 014 下午 10:03
 **/
@Configuration
@Slf4j
public class MvcConfig implements WebMvcConfigurer {

    @Resource
    RsshubConfig rsshubConfig;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        try {
            registry.addResourceHandler("/image/**").addResourceLocations("file:"+rsshubConfig.getTempPath());
        }catch (Exception e){
            log.error(Arrays.toString(e.getStackTrace()));
        }
    }
}
