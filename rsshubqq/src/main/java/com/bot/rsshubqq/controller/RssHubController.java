package com.bot.rsshubqq.controller;


import com.bot.rsshubqq.config.RsshubConfig;
import com.bot.rsshubqq.config.TranslateConfig;
import com.bot.rsshubqq.mapper.RsshubMapper;
import com.bot.rsshubqq.pojo.RssFeedItem;
import com.bot.rsshubqq.pojo.RssResult;
import com.bot.rsshubqq.service.RssHubService;
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

    /**存放执行线程的Array*/
    ArrayList<RssHubService> threads=new ArrayList<>();

    ArrayList<Thread> threadArrayList=new ArrayList<>();

    @SneakyThrows
    @Override
    public synchronized void run() {
        while(!Thread.currentThread().isInterrupted()) {
            clearTempFile();//清空临时文件夹
            log.info("开始抓取RssFeed："+rsshubFeedConfig.getRssList().stream().map(RssFeedItem::getName).collect(Collectors.toList()));
            threads.clear();//清空线程所在
            threadArrayList.clear();
            for (RssFeedItem item : rsshubFeedConfig.getRssList()) {
                RssResult rssResult = rsshubMapper.getResult(item);//获取当前的响应结果
                RssHubService rssHubService=new RssHubService(item, rssResult,this,translateConfig,rsshubFeedConfig.getTempPath());
                Thread thread = new Thread(rssHubService);//构建新的独立抓取线程
                threads.add(rssHubService);
                threadArrayList.add(thread);
                thread.start();
                if(rsshubFeedConfig.getRssList().size()-1!=rsshubFeedConfig.getRssList().indexOf(item)) {
                    //非最后一个间隔休眠
                    Thread.sleep(rsshubFeedConfig.getItemPauseTime() * 1000L);
                }
            }
            log.debug("开始等待完成抓取");
            while (isPullUnFinish()){
                //当未抓取完时等待
                this.wait();
            }
            //完成抓取保存结果
            log.info("完成抓取");
            rsshubMapper.save();
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
    private void clearTempFile(){
        File file=new File(rsshubFeedConfig.getTempPath());
        if(file.exists()){
            File[] files=file.listFiles();
            for(File item:files){
                item.delete();
            }
        }
    }
}
