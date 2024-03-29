package com.bot.steamBranch.config;

import com.bot.steamBranch.controller.SteamController;
import com.bot.steamBranch.service.SteamService;
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
 * @Description TODO Steam Schedule配置
 * @Date 2022/2/19 下午 8:14
 **/
@Component
@EnableScheduling
@Slf4j
public class SteamScheduleConfig implements SchedulingConfigurer {

    @Resource
    SteamConfig steamConfig;

    @Resource
    SteamController steamController;

    Thread steamThread;

    /**
     * 配置RssHub的定时抓取
     *
     * @param taskRegistrar 注入任务注册器
     */
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addTriggerTask(() -> {
            log.debug("定时任务线程运行");
            if (steamConfig.isEnable()) {//若开启RssHubQQ抓取
                if (steamThread != null && steamController.isPullUnFinish()) {
                    if (!steamController.isPullUnFinish()) {
                        log.error("陷入等待检测死锁");
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        for (SteamService item : steamController.getThreads()) {
                            if (!item.isFinished()) {
                                stringBuilder.append(item.getSteamFeedItem().getName()).append(" ");
                            }
                        }
                        steamController.suspendAll();
                        synchronized (steamController) {
                            steamController.notify();
                        }
                        log.error("抓取超时至一下个抓取循环（可能抓取间隔过小）:" + stringBuilder);
                    }
                } else {
                    if (steamThread == null || !steamThread.isAlive()) {
                        //无线程，新建并启动
                        steamThread = new Thread(steamController);
                        steamThread.setName("steam");
                        steamThread.start();
                    } else {
                        //有线程则唤醒
                        synchronized (steamController) {
                            steamController.notify();
                        }
                    }
                }
            }
        }, triggerContext -> {
            //动态触发器
            PeriodicTrigger periodicTrigger = new PeriodicTrigger(1000L * steamConfig.getQueryTime());
            return periodicTrigger.nextExecutionTime(triggerContext);
        });
    }
}
