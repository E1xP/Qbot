package com.bot.steamBranch.pojo;

import lombok.Data;

import java.util.List;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.steamBranch.pojo
 * @CLASS_NAME SteamFeedItem
 * @Description TODO Steam抓取来源类
 * @Date 2022/10/20 020 下午 11:24
 **/
@Data
public class SteamFeedItem {
    /**
     * 名称
     */
    String name;
    /**
     * Steam app id
     */
    Long appId;
    /**
     * branch列表
     */
    List<String> branchList;
    /**
     * 发送群id列表
     */
    List<Long> groupList;
}
