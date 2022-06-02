package com.bot.rsshubqq.pojo;

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

    public RssItem(SyndEntry syndEntry){
        this.description=syndEntry.getDescription().getValue();
        this.link=syndEntry.getLink();
        this.pubDate=syndEntry.getPublishedDate();
    }

    public RssItem(String description,String link,Date pubDate){
        this.description=description;
        this.link=link;
        this.pubDate=pubDate;
    }

    public RssItem(){}
}
