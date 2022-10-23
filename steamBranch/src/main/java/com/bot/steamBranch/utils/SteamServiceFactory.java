package com.bot.steamBranch.utils;

import com.bot.steamBranch.controller.SteamController;
import com.bot.steamBranch.pojo.SteamFeedItem;
import com.bot.steamBranch.service.SteamService;
import lombok.NonNull;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.steamBranch.utils
 * @CLASS_NAME SteamServiceFactory
 * @Description TODO Steam Service工厂类
 * @Date 2022/10/22 022 下午 8:50
 **/
@Component
public class SteamServiceFactory {

    @Resource
    private ApplicationContext applicationContext;

    /**
     * 获取SteamService实例类
     *
     * @param steamFeedItem   抓取源
     * @param steamController SteamController
     * @return SteamService
     */
    public SteamService getSteamService(@NonNull SteamFeedItem steamFeedItem, @NonNull SteamController steamController) {
        SteamService bean = applicationContext.getBean(SteamService.class);
        bean.setSteamFeedItem(steamFeedItem);
        bean.setSteamController(steamController);
        return bean;
    }
}
