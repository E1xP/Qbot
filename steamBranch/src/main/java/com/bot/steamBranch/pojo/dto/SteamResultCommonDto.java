package com.bot.steamBranch.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.steamBranch.pojo.dto
 * @CLASS_NAME SteamResultCommonDto
 * @Description TODO Steam结果的CommonDto
 * @Date 2022/10/23 023 下午 6:02
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SteamResultCommonDto {
    String name;
    String type;
    String osList;
    String osarch;
    String icon;
    String logo;
    String logoSmall;
    String clienttga;
    String clienticon;
    String ReleaseState;
    String osextended;
    String exfgls;
    String hasAdultContent;
    String hasAdultContentSex;
    String hasAdultContentViolence;
    String metacriticName;
    String storeAssetMtime;
    String communityVisibleStats;
    String communityHubVisible;
    String gameid;
    String reviewScore;
    String reviewPercentage;
}
