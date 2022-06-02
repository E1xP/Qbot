package com.bot.rsshubqq.service;

import com.bot.rsshubqq.config.TranslateConfig;
import com.bot.rsshubqq.controller.RssHubController;
import com.bot.rsshubqq.pojo.RssFeedItem;
import com.bot.rsshubqq.pojo.RssItem;
import com.bot.rsshubqq.pojo.RssResult;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.rsshubqq.service
 * @CLASS_NAME RssHubService
 * @Description TODO 执行单个RSS源的抓取
 * @Date 2022/2/22 下午 10:30
 **/
@Slf4j
@Data
public class RssHubService implements Runnable {

    RssFeedItem rssFeedItem;

    RssResult rssResult;

    RssHubController rssHubController;

    TranslateConfig translateConfig;

    String tempPath;

    boolean finished=false;

    public RssHubService(RssFeedItem rssFeedItem, RssResult rssResult,RssHubController rssHubController,TranslateConfig translateConfig,String tempPath) {
        this.rssFeedItem = rssFeedItem;
        this.rssResult = rssResult;
        this.rssHubController=rssHubController;
        this.translateConfig=translateConfig;
        this.tempPath=tempPath;
    }

    /**
     * 运行线程
     */
    @SneakyThrows
    @Override
    public void run() {
        RestTemplate restTemplate = new RestTemplate();
        AtomicBoolean errorFlag = new AtomicBoolean(false);//抓取错误标识
        SyndFeed syndFeed=null;//抓取内容
        //抓取Rss内容
        try {
            log.debug(rssFeedItem.getName()+" = 开始抓取");
            syndFeed = restTemplate.execute(rssFeedItem.getUrl()+"?limit=10", HttpMethod.GET, null, response -> {
                SyndFeedInput input = new SyndFeedInput();
                try {
                    return input.build(new XmlReader(response.getBody()));
                } catch (FeedException e) {
                    //Feed无法解析
                    log.error(rssFeedItem.getName()+" = 无法解析URL请求的内容：\n" + response.getBody());
                    errorFlag.set(true);//设置抓取错误
                    return null;
                }
            });
        } catch (HttpClientErrorException|HttpServerErrorException e) {
            errorFlag.set(true);//设置抓取错误
            //抓取中出现网络错误
            log.error(rssFeedItem.getName()+" = 抓取网络错误：" + e.getStatusCode() + "\n" + e.getResponseBodyAsString());
        }catch (ResourceAccessException e){
            errorFlag.set(true);//设置抓取错误
            //抓取中出现网络错误
            log.error(rssFeedItem.getName()+" = 抓取网络错误：" + e.getMessage());
        }
        if (!errorFlag.get()&&syndFeed!=null) {//检查是否有抓取错误
            //对抓取内容进行处理
            ArrayList<RssItem> rssItems=new ArrayList<>();//抓取记录
            Date newLastDate=rssResult.getLastSendDate();//本次最新时间
            rssResult.setTitle(syndFeed.getTitle());//设置Feed Title
            ArrayList<RssItem> sendList=new ArrayList<>();//发送列表
            boolean isFirstTime=false;
            if(rssResult.getLastSendDate()==null){
                isFirstTime=true;
                log.info(rssFeedItem.getName()+" ==>首次抓取");
            }
            for(SyndEntry item:syndFeed.getEntries()){
                RssItem rssItem=new RssItem(item);
                rssItems.add(rssItem);
                if(!isFirstTime) {
                    //非首次抓取
                    if (rssResult.getLastSendDate().before(rssItem.getPubDate())) {
                        //发现新消息
                        sendList.add(rssItem);
                    }
                }
                if (newLastDate == null || newLastDate.before(rssItem.getPubDate())) {
                    //发现更新的时间
                    newLastDate = rssItem.getPubDate();
                }
            }
            rssResult.setRssItemList(rssItems);//加入抓取结果
            rssResult.setLastSendDate(newLastDate);//设置最后抓取时间
            //发送消息
            if(!sendList.isEmpty()){//有新消息
                log.info(rssResult.getName()+" ==>发现新消息"+sendList.size()+"条");
                 for(RssItem item:sendList){
                     //启动发送线程
                     log.debug(rssFeedItem.getName()+" = 启动发送新消息线程："+item.getLink());
                     Thread thread=new Thread(new RssHubSendService(rssResult.getTitle(),item,rssFeedItem,translateConfig,rssHubController.getRsshubFeedConfig(),tempPath));
                     thread.start();
                     if(sendList.size()-1!=sendList.indexOf(item)){
                         //非最后一个
                         Thread.sleep(500);
                     }
                 }
            }
        }
        log.debug(rssFeedItem.getName()+" = 完成抓取");
        synchronized (this) {
            this.setFinished(true);
        }
        //唤醒主线程检查是否均完成抓取
        synchronized (rssHubController){
            rssHubController.notify();
        }
    }

    private void clearTempFile(){
        File file=new File(tempPath);
        if(file.exists()){
            File[] files=file.listFiles();
            for(File item:files){
                item.delete();
            }
        }
    }
}
