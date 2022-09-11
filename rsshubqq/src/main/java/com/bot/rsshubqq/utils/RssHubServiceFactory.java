package com.bot.rsshubqq.utils;

import com.bot.rsshubqq.controller.RssHubController;
import com.bot.rsshubqq.pojo.RssFeedItem;
import com.bot.rsshubqq.service.RssHubService;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.rsshubqq.utils
 * @CLASS_NAME RssHubSendServiceFactory
 * @Description TODO
 * @Date 2022/9/11 011 下午 10:08
 **/
@Component
public class RssHubServiceFactory {

    @Resource
    private ApplicationContext applicationContext;

    /**
     * 获取RsshubService实例类
     *
     * @param feedItem         抓取源
     * @param rssHubController RssHubController
     * @return RssHubService
     */
    public RssHubService getRssHubService(RssFeedItem feedItem, RssHubController rssHubController) {
        RssHubService bean = applicationContext.getBean(RssHubService.class);
        bean.setRssFeedItem(feedItem);
        bean.setRssHubController(rssHubController);
        return bean;
    }
}
