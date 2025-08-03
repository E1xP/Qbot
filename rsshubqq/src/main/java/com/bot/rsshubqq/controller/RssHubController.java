package com.bot.rsshubqq.controller;


import com.bot.rsshubqq.config.RsshubConfig;
import com.bot.rsshubqq.config.TranslateConfig;
import com.bot.rsshubqq.mapper.RsshubMapper;
import com.bot.rsshubqq.pojo.RssFeedItem;
import com.bot.rsshubqq.service.RssHubService;
import com.bot.rsshubqq.utils.RssHubServiceFactory;
import com.bot.utils.service.EarlyWarningService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;

import javax.annotation.Resource;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.rsshubqq.controller
 * @CLASS_NAME RssHubController
 * @Description TODO 控制RssHub抓取（单次抓取线程
 * @Date 2022/2/22 下午 5:14
 **/
@Controller
@Slf4j
@Data
public class RssHubController {

    @Resource
    RsshubConfig rsshubFeedConfig;

    @Resource
    RsshubMapper rsshubMapper;

    @Resource
    TranslateConfig translateConfig;

    @Resource
    RssHubServiceFactory rssHubServiceFactory;

    @Resource
    EarlyWarningService earlyWarningService;

    // 添加存储每个Feed的错误信息
    private Map<String, Integer> feedErrorInfo = new HashMap<>();
    
    /**
     * 删除临时文件夹中指定时间之前的文件
     * 只删除距离现在超过两倍全局抓取时间的文件
     */
    public void clearOldTempFiles() {
        File tempDir = new File(rsshubFeedConfig.getTempPath());
        if (tempDir.exists() && tempDir.isDirectory()) {
            // 计算三倍全局抓取时间
            long threshold = System.currentTimeMillis() - (rsshubFeedConfig.getQueryTime() * 3 * 1000L);

            File[] files = tempDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    // 如果文件最后修改时间早于阈值，则删除
                    if (file.lastModified() < threshold) {
                        if (file.delete()) {
                            log.debug("已删除临时文件: {}", file.getName());
                        } else {
                            log.warn("删除临时文件失败: {}", file.getName());
                        }
                    }
                }
            }
        }
    }


    /**
     * 当遇到错误时
     */
    public void onError(String branchName, String errorMessage, String stackTrace) {
        synchronized (this) {
            // 存储错误信息
            int errorCount = 0;
            if (feedErrorInfo.containsKey(branchName)) {
                errorCount = feedErrorInfo.get(branchName);
            }
            errorCount++;

            // 发送单个Feed的错误告警
            if (rsshubFeedConfig.isBranchErrorInfo() && errorCount > rsshubFeedConfig.getBranchErrorInfoCount()) {
                earlyWarningService.sendEarlyWarning(
                        "RssHub-" + branchName + "抓取错误",
                        "RssHub-" + branchName + "抓取失败: \n" + errorMessage + "\n堆栈信息:\n" + stackTrace
                );
            }
            feedErrorInfo.put(branchName, errorCount);
        }
    }

    /**
     * 当成功时
     */
    public void onSuccess(String branchName) {
        synchronized (this) {
            int errorCount = 0;
            if (feedErrorInfo.containsKey(branchName)) {
                errorCount = feedErrorInfo.get(branchName);
            }

            // 发送恢复通知
            if (rsshubFeedConfig.isBranchErrorInfo() && errorCount > rsshubFeedConfig.getBranchErrorInfoCount()) {
                earlyWarningService.sendEarlyWarning(
                        "RssHub-" + branchName + "抓取恢复",
                        "RssHub-" + branchName + ":抓取错误已恢复"
                );
            }
            // 清除错误信息
            feedErrorInfo.remove(branchName);
        }
    }

    /**
     * 获取指定Feed的RssHubService实例
     */
    public RssHubService getRssHubService(RssFeedItem feedItem) {
        return rssHubServiceFactory.getRssHubService(feedItem, this);
    }
}
