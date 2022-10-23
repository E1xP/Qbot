package com.bot.steamBranch.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.steamBranch.pojo
 * @CLASS_NAME SteamFileEntity
 * @Description TODO Steam持久化实体类
 * @Date 2022/10/22 022 下午 5:37
 **/
@Data
@AllArgsConstructor
public class SteamFileEntity {

    private Map<String, SteamResult> resultMap;

    public SteamFileEntity() {
        resultMap = new HashMap<>();
    }
}
