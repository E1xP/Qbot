package com.bot.steamBranch.pojo;

import lombok.Data;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.steamBranch.pojo
 * @CLASS_NAME SteamBranchItem
 * @Description TODO SteamBranch抓取结果
 * @Date 2022/10/22 022 下午 5:40
 **/
@Data
public class SteamBranchItem {
    /**
     * Branch名称
     */
    String name;
    /**
     * unix时间戳
     */
    Long timeStamp;
    /**
     * 版本Id
     */
    String buildId;
}
