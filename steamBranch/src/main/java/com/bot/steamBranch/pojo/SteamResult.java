package com.bot.steamBranch.pojo;

import com.bot.steamBranch.pojo.dto.SteamResultBranchDto;
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

    public SteamResult(SteamFeedItem steamFeedItem, String title, Map<String, SteamResultBranchDto> branches) {
        this.name = steamFeedItem.getName();
        this.title = title;
        HashMap<String, SteamBranchItem> steamBranchItemMap = new HashMap<>();
        for (String branchName : branches.keySet()) {
            SteamBranchItem steamBranchItem = new SteamBranchItem();
            steamBranchItem.setName(branchName);
            steamBranchItem.setTimeStamp(branches.get(branchName).getTimeupdated());
            steamBranchItem.setBuildId(branches.get(branchName).getBuildid());
            steamBranchItemMap.put(branchName, steamBranchItem);
        }
        this.steamBranchItemMap = steamBranchItemMap;
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

    /**
     * 持久化抓取记录
     *
     * @param gameName 游戏名称
     * @param branches SteamResultBranchDto
     */
    public void setNewResult(String gameName, Map<String, SteamResultBranchDto> branches) {
        this.title = gameName;
        HashMap<String, SteamBranchItem> steamBranchItemMap = new HashMap<>();
        for (String branchName : branches.keySet()) {
            SteamBranchItem steamBranchItem = new SteamBranchItem();
            steamBranchItem.setName(branchName);
            steamBranchItem.setTimeStamp(branches.get(branchName).getTimeupdated());
            steamBranchItem.setBuildId(branches.get(branchName).getBuildid());
            steamBranchItem.setPublic(branches.get(branchName).getIsClose() == 0);
            steamBranchItemMap.put(branchName, steamBranchItem);
        }
        this.steamBranchItemMap = steamBranchItemMap;
    }
}
