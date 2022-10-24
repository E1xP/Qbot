package com.bot.steamBranch.service;

import com.bot.steamBranch.config.SteamConfig;
import com.bot.steamBranch.controller.SteamController;
import com.bot.steamBranch.mapper.SteamMapper;
import com.bot.steamBranch.pojo.SteamBranchItem;
import com.bot.steamBranch.pojo.SteamFeedItem;
import com.bot.steamBranch.pojo.SteamResult;
import com.bot.steamBranch.pojo.dto.SteamResultBranchDto;
import com.bot.steamBranch.pojo.dto.SteamResultDto;
import com.bot.steamBranch.utils.SteamSendServiceFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.*;
import java.util.Map;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.steamBranch.service
 * @CLASS_NAME SteamService
 * @Description TODO Steam Service
 * @Date 2022/10/18 018 下午 10:38
 **/

@Slf4j
@Data
@Scope("prototype")
@Service
public class SteamService implements Runnable {

    SteamFeedItem steamFeedItem;

    SteamController steamController;

    SteamResult steamResult;

    @Resource
    SteamMapper steamMapper;

    @Resource
    SteamSendServiceFactory steamSendServiceFactory;

    @Resource
    SteamConfig steamConfig;

    @Resource
    ObjectMapper objectMapper;

    boolean finished = false;

    public void setSteamFeedItem(SteamFeedItem steamFeedItem) {
        this.steamFeedItem = steamFeedItem;
        SteamResult steamResult = steamMapper.getResult(steamFeedItem);
        this.setSteamResult(steamResult);
    }

    /**
     * 运行线程
     */
    @Override
    public void run() {
        Runtime run = Runtime.getRuntime();
        try {
            //执行SteamCmd Shell脚本
            Process process = run.exec("bash " + steamConfig.getSteamCmdPath() + " +login anonymous +app_info_print " + steamFeedItem.getAppId() + " +quit");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(process.getOutputStream())), true);
            out.println("exit");
            String readLine = "";
            StringBuilder stringBuilder = new StringBuilder();
            while ((readLine = bufferedReader.readLine()) != null) {
                stringBuilder.append(readLine);
            }
            //处理V社格式为标准Json格式
            String result = stringBuilder.substring(stringBuilder.indexOf("{"), stringBuilder.lastIndexOf("}") + 1)
                    .replaceAll("[\t\n]", "")
                    .replaceAll("\"([^{}]*?)\"\"([^{}]*?)\"", "\"$1\":\"$2\"")
                    .replaceAll("\"([^{}]*?)\"\"([^{}]*?)\"", "\"$1\",\"$2\"")
                    .replaceAll("\"([^{}]*?)\"\\{", "\"$1\":{")
                    .replaceAll("}\"([^{}]*?)\"", "},\"$1\"");
            //若获取不到
            if (StringUtils.isEmpty(result)) {
                log.error(steamFeedItem.getName() + "获取不到对应App数据，请检查SteamCmd查询与AppId情况");
            } else {
                log.debug(steamFeedItem.getName() + "获取结果:" + result);
                //反序列化
                SteamResultDto steamResultDto = objectMapper.readValue(result, SteamResultDto.class);
                Map<String, SteamResultBranchDto> branches = steamResultDto.getDepots().getBranches();
                //判断是否有更新并构造Str
                StringBuilder updateBuilder = new StringBuilder();//结果Str
                SteamResult oldResult = steamMapper.getResult(steamFeedItem);
                int updateBranchCount = 0;
                for (String branchName : branches.keySet()) {
                    if (steamFeedItem.getBranchList().contains(branchName)) {
                        //若为监听分支
                        SteamResultBranchDto resultItem = branches.get(branchName);
                        SteamBranchItem oldBranchResult = oldResult.getOldBranchResult(branchName);
                        if (resultItem.getTimeupdated() > oldBranchResult.getTimeStamp()) {
                            //有新分支时间
                            updateBranchCount++;

                        }
                    }
                }
                if (updateBranchCount > 0) {
                    //有更新分支

                }
            }
            //等待运行完
            process.waitFor();
            bufferedReader.close();
            out.close();
            process.destroy();
        } catch (IOException e) {
            log.error("IO异常-" + steamFeedItem.getName() + ":" + e.getMessage());
        } catch (InterruptedException e) {
            log.error("中断异常-" + steamFeedItem.getName() + ":" + e.getMessage());
        }
        log.debug(steamFeedItem.getName() + " = 完成抓取");
        synchronized (this) {
            this.setFinished(true);
        }
        //唤醒主线程检查是否均完成抓取
        synchronized (steamController) {
            steamController.notify();
        }
    }
}
