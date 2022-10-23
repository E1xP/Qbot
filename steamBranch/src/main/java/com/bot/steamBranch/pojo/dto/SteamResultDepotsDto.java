package com.bot.steamBranch.pojo.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.steamBranch.pojo.dto
 * @CLASS_NAME SteamResultDepotsDto
 * @Description TODO Steam结果DepotsDto
 * @Date 2022/10/23 023 下午 8:40
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SteamResultDepotsDto {
    @JsonAlias("branches")
    Map<String, SteamResultBranchDto> branches;
}
