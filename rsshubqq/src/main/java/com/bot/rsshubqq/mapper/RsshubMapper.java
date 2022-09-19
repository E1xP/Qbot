package com.bot.rsshubqq.mapper;

import com.bot.rsshubqq.config.RsshubConfig;
import com.bot.rsshubqq.pojo.FileEntity;
import com.bot.rsshubqq.pojo.RssFeedItem;
import com.bot.rsshubqq.pojo.RssResult;
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
 * @PACKAGE_NAME com.bot.rsshubqq.mapper
 * @CLASS_NAME RsshubMapper
 * @Description TODO
 * @Date 2022/2/20 下午 12:04
 **/
@Component
@Slf4j
@Data
@DependsOn({"rsshubConfig"})
public class RsshubMapper {

    @Resource
    private RsshubConfig rsshubConfig;

    @Resource
    private ObjectMapper objectMapper;

    private FileEntity fileEntity;

    /**
     * 构造函数
     * @param rsshubConfig rsshub设置
     * @param objectMapper json读写Mapper
     */
    public RsshubMapper(RsshubConfig rsshubConfig,ObjectMapper objectMapper){
        this.rsshubConfig=rsshubConfig;
        this.objectMapper = objectMapper;
        fileEntity = new FileEntity();
        this.load();
    }

    /**
     * 根据RssFeedItem获取上次抓取结果
     * @param rssFeedItem rssFeed对象
     * @return 上次Rss抓取结果，若不存在则创建
     */
    public RssResult getResult(RssFeedItem rssFeedItem){
        RssResult rssResult = fileEntity.getResultMap().get(rssFeedItem.getName());
        if(rssResult==null){
            rssResult = new RssResult(rssFeedItem);
            fileEntity.getResultMap().put(rssResult.getName(), rssResult);
        }
        return rssResult;
    }

    /**
     * 用于持久化文件系统到文件
     */
    public void save(){
        try {
            File saveFile = new File(rsshubConfig.getDbPath());
            if (!saveFile.getParentFile().exists()) {
                //若目录不存在
                saveFile.getParentFile().mkdirs();
            }
            FileOutputStream fileOut = new FileOutputStream(rsshubConfig.getDbPath());
            synchronized (this) {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(fileOut, fileEntity);
            }
            fileOut.close();
            log.debug("已保存到:" + rsshubConfig.getDbPath());
        }catch(FileNotFoundException e){
            log.error("设置的文件路径有误："+ rsshubConfig.getDbPath());
        } catch (IOException e) {
            log.error("IO异常:"+ e);
        }
    }

    /**
     * 用于保存Rss数据
     */
    public void load(){
        try {
            FileInputStream fileIn = new FileInputStream(rsshubConfig.getDbPath());
            fileEntity = objectMapper.readValue(fileIn, FileEntity.class);
            fileIn.close();
            log.info("初始加载DB成功-文件路径："+rsshubConfig.getDbPath());
        } catch (FileNotFoundException e) {//若无该文件夹
            log.info("初始加载DB-无此目录文件："+rsshubConfig.getDbPath());
        }catch (IOException e){//IO异常（无类异常不可能抛出
            log.error("IO异常："+ e);
        }
    }

    public Map<String, RssResult> getResultMap() {
        return fileEntity.getResultMap();
    }

    public void setResultMap(Map<String, RssResult> resultMap) {
        fileEntity.setResultMap(resultMap);
    }
}
