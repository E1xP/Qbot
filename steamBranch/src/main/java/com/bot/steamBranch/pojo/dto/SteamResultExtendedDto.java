package com.bot.steamBranch.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.steamBranch.pojo.dto
 * @CLASS_NAME SteamResultExtendedDto
 * @Description TODO Steam结果ExtendedDto
 * @Date 2022/10/23 023 下午 8:32
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SteamResultExtendedDto {
    String developer;
    String publisher;
    String homepage;
    String listofdlc;
    String dlcavailableonstore;
}
