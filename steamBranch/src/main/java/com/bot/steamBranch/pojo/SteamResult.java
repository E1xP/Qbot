package com.bot.steamBranch.pojo;

import com.bot.steamBranch.pojo.dto.SteamResultDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.HashMap;
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
    Map<String, SteamBranchItem> steamBranchItemMap = null;

    public SteamResult(SteamFeedItem steamFeedItem) {
        this.name = steamFeedItem.getName();
    }

    public SteamResult(SteamFeedItem steamFeedItem, SteamResultDto resultDto) {
        this.name = steamFeedItem.getName();
        this.title = resultDto.getCommonDto().getName();
        this.steamBranchItemMap = new HashMap<>();
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

    /**
     * 设置对应branch以往抓取结果
     *
     * @param steamBranchItem 老抓取结果
     */
    public void setOldBranchResult(SteamBranchItem steamBranchItem) {
        steamBranchItemMap.put(steamBranchItem.getName(), steamBranchItem);
    }

    @JsonIgnore
    public boolean isEmpty() {
        return getSteamBranchItemMap().isEmpty();
    }
}
