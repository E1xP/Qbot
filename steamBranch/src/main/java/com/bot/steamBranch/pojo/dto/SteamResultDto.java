package com.bot.steamBranch.pojo.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.steamBranch.pojo
 * @CLASS_NAME SteamResultDto
 * @Description TODO Steam结果DTO类
 * @Date 2022/10/23 023 下午 5:57
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SteamResultDto {
    @JsonAlias("common")
    SteamResultCommonDto commonDto;
    @JsonAlias("extended")
    SteamResultExtendedDto extended;
    @JsonAlias("depots")
    SteamResultDepotsDto depots;
}
