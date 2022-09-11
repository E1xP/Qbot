package com.bot.rsshubqq.utils;

import com.bot.rsshubqq.pojo.RssFeedItem;
import com.bot.rsshubqq.pojo.RssItem;
import com.bot.rsshubqq.service.RssHubSendService;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.rsshubqq.utils
 * @CLASS_NAME RssHubSendServiceFactory
 * @Description TODO
 * @Date 2022/9/11 011 下午 10:33
 **/
@Component
public class RssHubSendServiceFactory {

    @Resource
    private ApplicationContext applicationContext;

    public RssHubSendService getRssHubSendService(String title, RssItem rssItem, RssFeedItem rssFeedItem) {
        RssHubSendService bean = applicationContext.getBean(RssHubSendService.class);
        bean.setSendName(title);
        bean.setSendItem(rssItem);
        bean.setRssFeedItem(rssFeedItem);
        return bean;
    }

}
