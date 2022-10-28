package com.bot.rsshubqq.config;

import com.bot.rsshubqq.controller.RssHubController;
import com.bot.rsshubqq.service.RssHubService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

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

    Thread rsshubThread;

    /**
     * 配置Rsshub的定时抓取
     * @param taskRegistrar 注入任务注册器
     */
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addTriggerTask(() -> {
            log.debug("定时任务线程运行");
            if (rsshubFeedConfig.isEnable()) {//若开启RssHubQQ抓取
                if(rsshubThread!=null&&rssHubController.isPullUnFinish()){
                    if(!rssHubController.isPullUnFinish()){
                        log.error("陷入等待检测死锁");
                    }else{
                        StringBuilder stringBuilder=new StringBuilder();
                        for(RssHubService item :rssHubController.getThreads()) {
                            if (!item.isFinished()) {
                                stringBuilder.append(item.getRssFeedItem().getName()).append(" ");
                            }
                        }
                        rssHubController.suspendAll();
                        synchronized (rssHubController){
                            rssHubController.notify();
                        }
                        log.error("抓取超时至一下个抓取循环（可能抓取间隔过小）:"+stringBuilder);
                    }
                }else{
                    if(rsshubThread==null||!rsshubThread.isAlive()) {
                        //无线程，新建并启动
                        rsshubThread = new Thread(rssHubController);
                        rsshubThread.setName("RssHubPull");
                        rsshubThread.start();
                    }else{
                        //有线程则唤醒
                        synchronized (rssHubController){
                            rssHubController.notify();
                        }
                    }
                }
            }
        }, triggerContext -> {
            //动态触发器
            PeriodicTrigger periodicTrigger=new PeriodicTrigger(1000L *rsshubFeedConfig.getQueryTime());
            return periodicTrigger.nextExecutionTime(triggerContext);
        });
    }
}
