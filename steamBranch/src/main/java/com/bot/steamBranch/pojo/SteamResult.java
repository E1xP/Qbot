package com.bot.steamBranch.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.Map;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.steamBranch.pojo
 * @CLASS_NAME SteamResult
 * @Description TODO steam游戏更新抓取结果
 * @Date 2022/10/22 022 下午 5:38
 **/
@Data
public class SteamResult {
    String name;
    @JsonIgnore
    String title;
    /**
     * 抓取结果列表
     */
    Map<String, SteamBranchItem> steamBranchItemMap;

    public SteamResult(SteamFeedItem steamFeedItem) {
        this.name = steamFeedItem.getName();
    }

    public SteamResult() {
    }

    /**
     * 获取对应branch以往抓取结果
     *
     * @param branchName Branch名称
     * @return branch以往抓取结果
     */
    public SteamBranchItem getOldBranchResult(String branchName) {
        return steamBranchItemMap.get(branchName);
    }

    @JsonIgnore
    public boolean isEmpty() {
        return getSteamBranchItemMap().isEmpty();
    }
}
