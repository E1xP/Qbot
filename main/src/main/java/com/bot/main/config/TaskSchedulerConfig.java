package com.bot.main.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.main.config
 * @CLASS_NAME TaskSchedulerConfig
 * @Description TODO TaskScheduler
 * @Date 2022/10/28 028 下午 10:18
 **/
@Configuration
public class TaskSchedulerConfig {
    /**
     * 注入任务管理器
     *
     * @return TaskScheduler
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setThreadNamePrefix("taskScheduler-");
        taskScheduler.setPoolSize(10);
        taskScheduler.setRemoveOnCancelPolicy(true);
        return taskScheduler;
    }
}
