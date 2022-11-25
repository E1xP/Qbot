package com.bot.steamBranch.pojo.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.steamBranch.pojo.dto
 * @CLASS_NAME SteamResultBranchDto
 * @Description TODO Steam结果BranchDto
 * @Date 2022/10/23 023 下午 8:41
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SteamResultBranchDto {
    String buildid;
    Long timeupdated;
    /**
     * 是否是封闭版本
     */
    @JsonAlias("pwdrequired")
    int isClose = 0;
}
