package com.bot.rsshubqq.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.rsshubqq.pojo
 * @CLASS_NAME RssResult
 * @Description TODO
 * @Date 2022/2/20 下午 11:40
 **/
@Data
public class RssResult{
    String name;
    @JsonIgnore
    String title;
    Date lastSendDate;
    List<RssItem> rssItemList;

    public RssResult(RssFeedItem rssFeedItem) {
        this.name=rssFeedItem.getName();
    }

    public RssResult(){}

    @JsonIgnore
    public boolean isEmpty(){
        return getRssItemList().isEmpty();
    }
}
