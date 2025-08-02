package com.bot.rsshubqq.config;

import com.bot.rsshubqq.controller.RssHubController;
import com.bot.rsshubqq.pojo.RssFeedItem;
import com.bot.rsshubqq.service.RssHubService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.rsshubqq.service
 * @CLASS_NAME RsshubScheduleServer
 * @Description TODO 配置RssHub抓取Schedule
 * @Date 2022/2/19 下午 8:14
 **/
@Component
@EnableScheduling
@Slf4j
public class RsshubScheduleConfig implements SchedulingConfigurer {

    @Resource
    RsshubConfig rsshubFeedConfig;

    @Resource
    RssHubController rssHubController;

    // 用于存储每个Feed正在运行的服务实例
    private final Map<String, RssHubService> runningServices = new ConcurrentHashMap<>();

    // 用于存储每个Feed正在运行的线程
    private final Map<String, Thread> runningThreads = new ConcurrentHashMap<>();

    /**
     * 配置RssHub的定时抓取
     * @param taskRegistrar 注入任务注册器
     */
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        // 为每个Feed配置独立的定时任务，添加延迟以避免抓取集中
        int index = 0;
        for (RssFeedItem feedItem : rsshubFeedConfig.getRssList()) {
            final int currentIndex = index;
            long fixedDelay = feedItem.getQueryTime() > 0 ? feedItem.getQueryTime() * 1000L : rsshubFeedConfig.getQueryTime() * 1000L;
            long initialDelay = currentIndex * rsshubFeedConfig.getItemPauseTime() * 1000L;

            //初始触发延迟
            PeriodicTrigger trigger = new PeriodicTrigger(fixedDelay, TimeUnit.MILLISECONDS);
            trigger.setInitialDelay(initialDelay);
            trigger.setFixedRate(false); // 使用固定延迟而不是固定频率

            taskRegistrar.addTriggerTask(() -> {


                log.debug("定时任务线程运行: " + feedItem.getName());
                if (rsshubFeedConfig.isEnable()) {
                    // 检查是否已有任务在运行（超时情况）
                    RssHubService existingService = runningServices.get(feedItem.getName());
                    if (existingService != null && !existingService.isFinished()) {
                        log.error("Feed {} 抓取超时，上一个任务仍未完成", feedItem.getName());
                        // 中断超时运行的任务线程
                        Thread existingThread = runningThreads.get(feedItem.getName());
                        if (existingThread != null && existingThread.isAlive()) {
                            log.error("正在中断Feed {} 的超时线程: {}", feedItem.getName(), existingThread.getName());
                            existingThread.interrupt();
                        }
                    }

                    RssHubService rssHubService = rssHubController.getRssHubService(feedItem);
                    if (rssHubService != null) {
                        // 记录正在运行的服务实例
                        runningServices.put(feedItem.getName(), rssHubService);

                        Thread thread = new Thread(rssHubService);
                        thread.setName("RssHub-" + feedItem.getName());
                        // 记录正在运行的线程
                        runningThreads.put(feedItem.getName(), thread);
                        thread.start();
                    }
                }
            }, trigger);

            index++;
        }
    }
}