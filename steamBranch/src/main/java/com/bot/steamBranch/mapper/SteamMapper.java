package com.bot.steamBranch.mapper;

import com.bot.steamBranch.config.SteamConfig;
import com.bot.steamBranch.pojo.SteamFeedItem;
import com.bot.steamBranch.pojo.SteamFileEntity;
import com.bot.steamBranch.pojo.SteamResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.*;
import java.util.Map;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.steamBranch.mapper
 * @CLASS_NAME SteamMapper
 * @Description TODO Steam Mapper
 * @Date 2022/10/18 018 下午 10:25
 **/
@Component
@Data
@Slf4j
@DependsOn({"steamConfig"})
public class SteamMapper {

    @Resource
    private SteamConfig steamConfig;

    @Resource
    private ObjectMapper objectMapper;

    private SteamFileEntity steamFileEntity;

    /**
     * 构造函数
     *
     * @param steamConfig  steam设置
     * @param objectMapper json读写Mapper
     */
    public SteamMapper(SteamConfig steamConfig, ObjectMapper objectMapper) {
        this.steamConfig = steamConfig;
        this.objectMapper = objectMapper;
        steamFileEntity = new SteamFileEntity();
        this.load();
    }

    /**
     * 根据SteamFeedItem获取上次抓取结果
     *
     * @param steamFeedItem SteamFeed对象
     * @return 上次Steam抓取结果，若不存在则创建
     */
    public SteamResult getResult(SteamFeedItem steamFeedItem) {
        SteamResult steamResult = steamFileEntity.getResultMap().get(steamFeedItem.getName());
        if (steamResult == null) {
            steamResult = new SteamResult(steamFeedItem);
            steamFileEntity.getResultMap().put(steamResult.getName(), steamResult);
        }
        return steamResult;
    }

    /**
     * 用于持久化文件系统到文件
     */
    public void save() {
        try {
            File saveFile = new File(steamConfig.getDbPath());
            if (!saveFile.getParentFile().exists()) {
                //若目录不存在
                saveFile.getParentFile().mkdirs();
            }
            FileOutputStream fileOut = new FileOutputStream(steamConfig.getDbPath());
            synchronized (this) {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(fileOut, steamFileEntity);
            }
            fileOut.close();
            log.debug("已保存到:" + steamConfig.getDbPath());
        } catch (FileNotFoundException e) {
            log.error("设置的文件路径有误：" + steamConfig.getDbPath());
        } catch (IOException e) {
            log.error("IO异常:" + e);
        }
    }

    /**
     * 用于保存Steam抓取记录
     */
    public void load() {
        try {
            FileInputStream fileIn = new FileInputStream(steamConfig.getDbPath());
            steamFileEntity = objectMapper.readValue(fileIn, SteamFileEntity.class);
            fileIn.close();
            log.info("初始加载DB成功-文件路径：" + steamConfig.getDbPath());
        } catch (FileNotFoundException e) {//若无该文件夹
            log.info("初始加载DB-无此目录文件：" + steamConfig.getDbPath());
        } catch (IOException e) {//IO异常（无类异常不可能抛出
            log.error("IO异常：" + e);
        }
    }

    public Map<String, SteamResult> getResultMap() {
        return steamFileEntity.getResultMap();
    }

    public void setResultMap(Map<String, SteamResult> resultMap) {
        steamFileEntity.setResultMap(resultMap);
    }
}
