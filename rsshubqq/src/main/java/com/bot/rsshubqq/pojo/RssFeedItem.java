package com.bot.rsshubqq.pojo;

import lombok.Data;

import java.util.List;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.qbot.rsshubqq.pojo
 * @CLASS_NAME RssItem
 * @Description TODO
 * @Date 2022/2/18 下午 2:20
 **/
@Data
public class RssFeedItem {
    String name;
    String url;
    List<Long> groups;
    boolean translate;
    boolean proxy;
    boolean feedProxy;
    boolean twitterRTFilter;
    boolean twitterREFilter;
}
