package com.bot.steamBranch.config;

import com.bot.steamBranch.pojo.SteamFeedItem;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.steamBranch.config
 * @CLASS_NAME SteamFeedConfig
 * @Description TODO steamBranch配置
 * @Date 2022/10/18 018 下午 10:21
 **/
@Data
@Configuration("steamConfig")
@ConfigurationProperties(prefix = "steam")
public class SteamConfig {
    /**
     * 是否开启steam版本的抓取
     */
    boolean enable;
    /**
     * 查询间隔时间
     */
    int queryTime;
    /**
     * Item查询间隔时间
     */
    int itemPauseTime;
    /**
     * steamCmd的运行shell路径
     */
    String steamCmdPath;
    /**
     * Steam版本数据库存储位置
     */
    String dbPath;
    /**
     * SteamFeed列表
     */
    List<SteamFeedItem> steamList;
    /**
     * steam用户名
     */
    String steamUserName;
    /**
     * 连续失败通知
     */
    boolean errorInfo;
    /**
     * 连续失败通知阈值
     */
    int errorInfoCount = 0;
}
