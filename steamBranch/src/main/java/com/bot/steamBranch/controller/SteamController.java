package com.bot.steamBranch.controller;

import com.bot.steamBranch.config.SteamConfig;
import com.bot.steamBranch.mapper.SteamMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;

import javax.annotation.Resource;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.steamBranch.controller
 * @CLASS_NAME SteamController
 * @Description TODO Steam Controller
 * @Date 2022/10/18 018 下午 10:25
 **/
@Controller
@Slf4j
@Data
public class SteamController implements Runnable {

    @Resource
    SteamConfig steamConfig;

    @Resource
    SteamMapper steamMapper;

    @Resource


    @Override
    public void run() {

    }
}
