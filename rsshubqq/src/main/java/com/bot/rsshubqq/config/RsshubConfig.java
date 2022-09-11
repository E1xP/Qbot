package com.bot.rsshubqq.config;

import com.bot.rsshubqq.pojo.RssFeedItem;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.rsshubqq.config
 * @CLASS_NAME RsshubFeedConfig
 * @Description TODO
 * @Date 2022/2/19 下午 4:35
 **/
@Data
@Configuration("rsshubConfig")
@ConfigurationProperties(prefix = "rsshub")
public class RsshubConfig {
    /**
     * 是否开启rsshub的抓取
     */
    boolean enable;
    /**
     * 查询循环时间
     */
    int queryTime;
    /**
     * RssItem查询间隔时间
     */
    int itemPauseTime;
    /**
     * RSS的查询Feed与配置
     */
    List<RssFeedItem> rssList;
    /**
     * RSS数据库存储位置
     */
    String dbPath;
    /**
     * RSS临时文件存储位置
     */
    String tempPath;
    /**
     * 本地访问URL地址
     */
    String localUrl;
    /**
     * 外部访问端口
     */
    int accessPort;
    /**
     * 是否开启临时文件访问
     */
    Boolean urlTempAccess;
    /**
     * 图片代理地址
     */
    String proxyUrl;
    /**
     * 图片代理端口
     */
    int proxyPort;
    /**
     * 是否在图片下载失败时通知
     */
    boolean downloadFailNotify;
    /**
     * 图片下载失败通知
     */
    Long downloadFailNotifier;
}
