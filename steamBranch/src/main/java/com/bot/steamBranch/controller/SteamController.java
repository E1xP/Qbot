package com.bot.steamBranch.controller;

import com.bot.steamBranch.config.SteamConfig;
import com.bot.steamBranch.mapper.SteamMapper;
import com.bot.steamBranch.pojo.SteamFeedItem;
import com.bot.steamBranch.service.SteamService;
import com.bot.steamBranch.utils.SteamServiceFactory;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.steamBranch.controller
 * @CLASS_NAME SteamController
 * @Description TODO Steam Controller
 * @Date 2022/10/18 018 下午 10:25
 **/
@Controller
@Slf4j
@Data
public class SteamController implements Runnable {

    @Resource
    SteamConfig steamConfig;

    @Resource
    SteamMapper steamMapper;

    @Resource
    SteamServiceFactory steamServiceFactory;

    /**
     * 存放执行线程的Array
     */
    ArrayList<SteamService> threads = new ArrayList<>();

    ArrayList<Thread> threadArrayList = new ArrayList<>();

    @SneakyThrows
    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            log.info("开始抓取RssFeed：" + steamConfig.getSteamList().stream().map(SteamFeedItem::getName).collect(Collectors.toList()));
            threads.clear();//清空线程所在
            threadArrayList.clear();
            for (SteamFeedItem item : steamConfig.getSteamList()) {
                SteamService steamService = steamServiceFactory.getSteamService(item, this);
                Thread thread = new Thread(steamService);//构建新的独立抓取线程
                threads.add(steamService);
                threadArrayList.add(thread);
                thread.start();
                if (steamConfig.getSteamList().size() - 1 != steamConfig.getSteamList().indexOf(item)) {
                    //非最后一个间隔休眠
                    Thread.sleep(steamConfig.getItemPauseTime() * 1000L);
                }
            }
            log.debug("开始等待完成抓取");
            while (isPullUnFinish()) {
                //当未抓取完时等待
                this.wait();
            }
            //完成抓取保存结果
            log.info("完成抓取");
            steamMapper.save();
            this.wait();
        }
    }

    /**
     * 强制终止所有执行中的线程
     */
    public void suspendAll() {
        synchronized (this) {
            for (Thread item : threadArrayList) {
                if (!item.isAlive()) {
                    item.stop();
                }
            }
            for (SteamService item : threads) {
                item.setFinished(true);
            }
        }
    }

    /**
     * 判断线程是否全部完成执行
     *
     * @return 是否完成执行
     */
    public boolean isPullUnFinish() {
        synchronized (this) {//获取自己，防止执行过程中被唤醒，陷入等待检查完成的死锁
            for (SteamService item : threads) {
                synchronized (item) {
                    if (!item.isFinished())
                        return true;
                }
            }
            return false;
        }
    }
}
