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
import com.bot.utils.CoolQUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.lz1998.cq.retdata.ApiData;
import net.lz1998.cq.retdata.MessageData;
import net.lz1998.cq.robot.CoolQ;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
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

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

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
            log.debug(steamFeedItem.getName() + "原始结果:" + stringBuilder);
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
                StringBuilder resultStr = new StringBuilder();//结果Str
                SteamResult oldResult = steamMapper.getResult(steamFeedItem);
                int updateBranchCount = 0;
                boolean newFlag = false;
                if (oldResult == null) {
                    //第一次获取
                    log.info(steamFeedItem.getName() + " ==>首次抓取");
                    SteamResult steamResult = new SteamResult(steamFeedItem, steamResultDto);
                    steamMapper.getResultMap().put(steamResult.getName(), steamResult);
                } else {
                    for (String branchName : branches.keySet()) {
                        if (steamFeedItem.getBranchList().contains(branchName)) {
                            //若为监听分支
                            SteamResultBranchDto resultItem = branches.get(branchName);
                            SteamBranchItem oldBranchResult = oldResult.getOldBranchResult(branchName);
                            if (oldBranchResult == null) {
                                //第一次获取
                                log.info(steamFeedItem.getName() + " ==>首次抓取分支-" + branchName);
                                SteamBranchItem steamBranchItem = new SteamBranchItem();
                                steamBranchItem.setName(branchName);
                                steamBranchItem.setBuildId(resultItem.getBuildid());
                                steamBranchItem.setTimeStamp(resultItem.getTimeupdated());
                                steamResult.setOldBranchResult(steamBranchItem);
                                newFlag = true;
                            } else {
                                //非第一次获取
                                if (resultItem.getTimeupdated() > oldBranchResult.getTimeStamp()) {
                                    //有新分支时
                                    if (updateBranchCount != 0) {
                                        //多结果增加分割线
                                        resultStr.append("----------------------\n");
                                    }
                                    resultStr.append(branchName).append(":\n")
                                            .append("\t版本号：").append(resultItem.getBuildid()).append("\n")
                                            .append("\t更新时间：").append(simpleDateFormat.format(new Date(resultItem.getTimeupdated()))).append("\n")
                                            .append("\t旧版本号：").append(oldBranchResult.getBuildId()).append("\n")
                                            .append("\t上次更新时间：").append(simpleDateFormat.format(new Date(oldBranchResult.getTimeStamp()))).append("\n");
                                    updateBranchCount++;
                                    //持久化更新结果
                                    oldBranchResult.setBuildId(resultItem.getBuildid());
                                    oldBranchResult.setTimeStamp(resultItem.getTimeupdated());
                                }
                            }
                        }
                    }
                    if (updateBranchCount > 0) {
                        //有更新分支
                        StringBuilder sendStrBuilder = new StringBuilder();
                        Date currentDate = new Date();
                        sendStrBuilder
                                .append("【").append(steamResultDto.getCommonDto().getName()).append("】Steam更新了!\n")
                                .append("共有").append(updateBranchCount).append("个分支更新\n")
                                .append("======================\n")
                                .append(resultStr)
                                .append("======================\n")
                                .append("采集时间:").append(simpleDateFormat.format(currentDate));
                        //持久化结果
                        steamMapper.save();
                        //向群发送结果
                        CoolQ coolQ = null;
                        int sendTryCount = 0;
                        String content = new String(stringBuilder);
                        String sendName = steamFeedItem.getName();
                        do {
                            coolQ = CoolQUtils.getCoolQ();
                            if (coolQ == null) {
                                sendTryCount++;
                                log.error(sendName + " = 发送获取不到Bot实体，延迟60s后再尝试发送");
                                Thread.sleep(1000 * 60);
                            }
                        } while (coolQ == null && sendTryCount < 5);//仅重试5次
                        if (coolQ != null) {
                            log.debug("开始发送消息：" + steamFeedItem.getName() + "\n群：" + steamFeedItem.getGroupList() + "\n内容：" + content);
                            int sendCount = 0;
                            ApiData<MessageData> apiData = null;
                            for (long groupId : steamFeedItem.getGroupList()) {
                                apiData = coolQ.sendGroupMsg(groupId, content, false);
                                //发送后判断单个消息
                                if (apiData.getStatus().equals("ok") && apiData.getRetcode() == 0) {
                                    sendCount++;
                                    log.debug(sendName + " = 发送群消息：" + groupId + "，成功：" + apiData);
                                } else {
                                    log.debug(sendName + " = 发送群消息：" + groupId + "，失败：" + apiData);
                                }
                                if (steamFeedItem.getGroupList().size() - 1 != steamFeedItem.getGroupList().indexOf(groupId)) {
                                    Thread.sleep(100);
                                }
                            }
                            if (sendCount == steamFeedItem.getGroupList().size()) {
                                log.info(sendName + " ==>完成发送：" + steamFeedItem.getName());
                            } else {
                                log.error(sendName + " = 发送失败：" + steamFeedItem.getName() + "\n返还消息：" + apiData + "\n消息内容：" + content);
                            }
                        } else {
                            log.error(sendName + " = 等待Bot5次失败放弃发送：" + steamFeedItem.getName());
                        }
                    }
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
