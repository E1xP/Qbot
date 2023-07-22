package com.bot.rsshubqq.controller;


import com.bot.rsshubqq.config.RsshubConfig;
import com.bot.rsshubqq.config.TranslateConfig;
import com.bot.rsshubqq.mapper.RsshubMapper;
import com.bot.rsshubqq.pojo.RssFeedItem;
import com.bot.rsshubqq.service.RssHubService;
import com.bot.rsshubqq.utils.RssHubServiceFactory;
import com.bot.utils.service.EarlyWarningService;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.rsshubqq.controller
 * @CLASS_NAME RssHubController
 * @Description TODO 控制RssHub抓取（单次抓取线程
 * @Date 2022/2/22 下午 5:14
 **/
@Controller
@Slf4j
@Data
public class RssHubController implements Runnable{

    @Resource
    RsshubConfig rsshubFeedConfig;

    @Resource
    RsshubMapper rsshubMapper;

    @Resource
    TranslateConfig translateConfig;

    @Resource
    RssHubServiceFactory rssHubServiceFactory;

    @Resource
    EarlyWarningService earlyWarningService;

    int errorCount = 0;

    boolean onWarningFlag = false;//是否已告警过

    boolean isWarnThisPull = false;//本次是否告警

    /**
     * 存放执行线程的Array
     */
    ArrayList<RssHubService> threads = new ArrayList<>();

    ArrayList<Thread> threadArrayList = new ArrayList<>();

    @SneakyThrows
    @Override
    public synchronized void run() {
        while (!Thread.currentThread().isInterrupted()) {
            isWarnThisPull = false;
            clearTempFile();//清空临时文件夹
            log.info("开始抓取RssFeed：" + rsshubFeedConfig.getRssList().stream().map(RssFeedItem::getName).collect(Collectors.toList()));
            threads.clear();//清空线程所在
            threadArrayList.clear();
            for (RssFeedItem item : rsshubFeedConfig.getRssList()) {
                RssHubService rssHubService = rssHubServiceFactory.getRssHubService(item, this);
                Thread thread = new Thread(rssHubService);//构建新的独立抓取线程
                thread.setName("RssHub");
                threads.add(rssHubService);
                threadArrayList.add(thread);
                thread.start();
                if(rsshubFeedConfig.getRssList().size()-1!=rsshubFeedConfig.getRssList().indexOf(item)) {
                    //非最后一个间隔休眠
                    Thread.sleep(rsshubFeedConfig.getItemPauseTime() * 1000L);
                }
            }
            log.debug("开始等待完成抓取");
            while (isPullUnFinish()) {
                //当未抓取完时等待
                synchronized (this) {
                    this.wait();
                }
            }
            //完成抓取保存结果
            log.info("完成抓取");
            rsshubMapper.save();

            //告警逻辑
            if (errorCount >= (rsshubFeedConfig.getErrorInfoCount() <= 0 ? rsshubFeedConfig.getRssList().size() : rsshubFeedConfig.getErrorInfoCount())) {
                //触发告警
                isWarnThisPull = true;
                if (!onWarningFlag) {
                    onWarningFlag = true;
                    earlyWarningService.sendEarlyWarning("本轮RssHub抓取已累计错误" + errorCount + "次！");
                }
                log.error("RssHub触发告警");
            } else {
                //告警恢复
                if (onWarningFlag) {
                    earlyWarningService.sendEarlyWarning("RssHub抓取错误已恢复");
                    log.info("RssHub告警已恢复");
                }
                onWarningFlag = false;
            }
            this.errorCount = 0;

            this.wait();
        }
    }

    /**
     * 判断线程是否全部完成执行
     * @return 是否完成执行
     */
    public boolean isPullUnFinish() {
        synchronized(this) {//获取自己，防止执行过程中被唤醒，陷入等待检查完成的死锁
            for (RssHubService item : threads) {
                synchronized (item) {
                    if (!item.isFinished())
                        return true;
                }
            }
            return false;
        }
    }

    /**
     * 强制终止所有执行中的线程
     */
    public void suspendAll(){
        synchronized (this){
            for(Thread item:threadArrayList){
                if(!item.isAlive()){
                    item.stop();
                }
            }
            for(RssHubService item:threads){
                item.setFinished(true);
            }
        }
    }

    /**
     * 删除临时文件中的文件
     */
    private void clearTempFile() {
        File file = new File(rsshubFeedConfig.getTempPath());
        if (file.exists()) {
            File[] files = file.listFiles();
            for (File item : files) {
                item.delete();
            }
        }
    }


    /**
     * 当遇到错误时
     */
    public void onError() {
        synchronized (this) {
            this.errorCount++;
        }
    }

    /**
     * 当成功时
     */
    public void onSuccess() {
//        synchronized (this) {
//            this.errorCount = 0;
//        }
    }
}
