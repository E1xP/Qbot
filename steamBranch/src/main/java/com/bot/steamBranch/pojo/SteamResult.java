package com.bot.steamBranch.pojo;

import com.bot.steamBranch.pojo.dto.SteamResultBranchDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.List;
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

    public void setResult(List<SteamResultBranchDto> steamResultBranchDtoList) {
        for (SteamResultBranchDto item : steamResultBranchDtoList) {

        }
    }

    public SteamResult() {
    }

    @JsonIgnore
    public boolean isEmpty() {
        return getSteamBranchItemMap().isEmpty();
    }
}
