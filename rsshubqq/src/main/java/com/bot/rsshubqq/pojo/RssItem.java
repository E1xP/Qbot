package com.bot.rsshubqq.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rometools.rome.feed.module.DCModuleImpl;
import com.rometools.rome.feed.module.Module;
import com.rometools.rome.feed.synd.SyndEntry;
import lombok.Data;

import java.util.Date;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.rsshubqq.pojo
 * @CLASS_NAME RssItem
 * @Description TODO
 * @Date 2022/2/20 下午 11:09
 **/
@Data
public class RssItem{
    /** 内容标题 */
    String description;
    /** 内容链接 */
    String link;
    /** 内容发布时间 */
    Date pubDate;
    /**
     * 转发来自
     */
    @JsonIgnore
    String author = null;
    /**
     * 是否转发
     */
    @JsonIgnore
    boolean isRT = false;
    /**
     * 是否回复
     */
    @JsonIgnore
    boolean isRE = false;

    public RssItem(SyndEntry syndEntry){
        this.description = syndEntry.getTitle() + (syndEntry.getDescription() != null ? syndEntry.getDescription().getValue() : "");
        this.link = syndEntry.getLink().replaceAll("nitter\\.([\\S]+\\.)+[\\S]+?\\/", "twitter.com/");
        this.pubDate=syndEntry.getPublishedDate();
        if (this.pubDate == null) {
            this.pubDate = syndEntry.getUpdatedDate();
        }
        if (syndEntry.getTitle().startsWith("RT")) {
            isRT = true;
        }
        if (syndEntry.getTitle().startsWith("Re")) {
            isRE = true;
        }
        if (!syndEntry.getModules().isEmpty()) {
            for (Module item : syndEntry.getModules()) {
                if (item instanceof DCModuleImpl) {
                    author = ((DCModuleImpl) item).getCreator();
                }
            }
        }
    }

    public RssItem(String description,String link,Date pubDate){
        this.description=description;
        this.link=link;
        this.pubDate=pubDate;
    }

    public RssItem(){}
}
