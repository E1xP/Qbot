package com.bot.steamBranch.utils;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.steamBranch.utils
 * @CLASS_NAME SteamSendServiceFactory
 * @Description TODO
 * @Date 2022/10/22 022 下午 8:58
 **/
@Component
public class SteamSendServiceFactory {

    @Resource
    private ApplicationContext applicationContext;

}
