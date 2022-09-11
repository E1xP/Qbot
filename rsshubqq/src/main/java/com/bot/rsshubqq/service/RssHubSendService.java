package com.bot.rsshubqq.service;

import com.bot.rsshubqq.config.RsshubConfig;
import com.bot.rsshubqq.config.TranslateConfig;
import com.bot.rsshubqq.pojo.RssFeedItem;
import com.bot.rsshubqq.pojo.RssItem;
import com.bot.rsshubqq.utils.BreakOnlyOne;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.lz1998.pbbot.bot.Bot;
import net.lz1998.pbbot.bot.BotContainer;
import net.lz1998.pbbot.utils.Msg;
import onebot.OnebotApi;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.rsshubqq.service
 * @CLASS_NAME RssHubSendService
 * @Description TODO
 * @Date 2022/2/25 下午 1:01
 **/
@Service
@Slf4j
@Data
@Scope("prototype")
public class RssHubSendService implements Runnable {

    /**
     * 获取bot的容器
     */
    @Resource
    BotContainer botContainer;

    /**
     * 发送名
     */
    String sendName;

    /**
     * 发送的信息对象
     */
    RssItem sendItem;

    /**
     * 发送设置
     */
    RssFeedItem rssFeedItem;

    /**
     * Rsshub设置
     */
    @Resource
    RsshubConfig rsshubConfig;

    /**
     * 翻译设置
     */
    @Resource
    TranslateConfig translateConfig;

    /**
     * 图片URL列表
     */
    ArrayList<String> imagesUrl;

    /**
     * 视频预览图URL列表
     */
    ArrayList<String> videosUrl;

    final SimpleDateFormat formatter = new SimpleDateFormat("MM月dd日 HH:mm:ss");

    String content;

    /**
     * 图片匹配Pattern
     */
    Pattern imagePattern = Pattern.compile("<img.+?src=\"(.+?)\".+?>");
    /**
     * 视频预览图匹配Pattern
     */
    Pattern videoPattern = Pattern.compile("<video.*?poster=\"(.+?)\".*?></video>");
    /**
     * 头部转发匹配Pattern
     */
    Pattern headerPattern = Pattern.compile("(?s)RT (.+?)<br>(.+)|(.+)$");

    RssHubSendService() {
    }

    @SneakyThrows
    @Override
    public void run() {
        content = sendItem.getDescription();
        //去除图片前换行
        content = content.replaceAll("<br>(<video.+?></video>)|<br>(<img.+?>)", "$1$2");
        Matcher headerMatcher=headerPattern.matcher(content);
        //构建标题头与文字信息
        String toTranslateMessage;
        if(headerMatcher.find()&&(headerMatcher.group(1)!=null)){//识别是否是转发
            content ="【"+sendName+"】转发了【"+headerMatcher.group(1)+
                    "】的消息!\n----------------------\n内容："
                    +headerMatcher.group(2)+"\n";
            toTranslateMessage=headerMatcher.group(2);
        }else{
            content ="【"+sendName+"】更新了!\n----------------------\n内容："
                    +headerMatcher.group(3)+"\n";
            toTranslateMessage=headerMatcher.group(3);
        }
        content = content.replaceAll("<br>","\n");//将换行替换为\n
        //翻译处理
        if(rssFeedItem.isTranslate()){
            toTranslateMessage=toTranslateMessage.replaceAll("<br>","\n");//将待翻译换行替换为\n
            toTranslateMessage=toTranslateMessage.replaceAll("</?[?a-zA-Z]+[^><]*>|&[a-zA-Z]{1,10}","");//删除图片或视频链接
            String translated = translate(toTranslateMessage);
            if(translated!=null) {
                log.debug(sendName+" = 翻译结果："+translated);
                content += "翻译：" + translated;
            }
        }
        //获取图片与视频预览图URL列表
        getMedia(content);
        content=content.replaceAll("</?[?a-zA-Z]+[^><]*>|&[a-zA-Z]{1,10}","");//删除图片或视频链接
        //处理媒体图片
        if (imagesUrl.size() != 0 || videosUrl.size() != 0) {

            log.debug(sendName + " = 图片链接为：" + imagesUrl);
            log.debug(sendName + " = 视频预览图链接为：" + videosUrl);

            content += "媒体：\n";
            StringBuilder stringBuilder = new StringBuilder();
            //存在媒体图片
            for (String item : videosUrl) {
                downAndAdd(item, stringBuilder);
            }
            for (String item : imagesUrl) {
                downAndAdd(item, stringBuilder);
            }
            content += stringBuilder;
        }
        content += "\n----------------------\n原链接：" + sendItem.getLink()
                + "\n日期：" + formatter.format(sendItem.getPubDate());
        content = BreakOnlyOne.onlyOneLineBreak(content);
        //发送消息
        Bot bot = null;
        int sendTryCount = 0;
        do {
            bot = getBot();
            if (bot == null) {
                sendTryCount++;
                log.error(sendName + " = 发送获取不到Bot实体，延迟60s后再尝试发送");
                Thread.sleep(1000 * 60);
            }
        } while (bot == null && sendTryCount < 5);//仅重试5次
        if (bot != null) {
            log.debug("开始发送消息：" + sendItem.getLink() + "\n群：" + rssFeedItem.getGroups() + "\n内容：" + content);
            int sendCount = 0;
            OnebotApi.SendGroupMsgResp sendGroupMsgResp = null;
            for (long groupId : rssFeedItem.getGroups()) {
                sendGroupMsgResp = bot.sendGroupMsg(groupId, content, false);
                //发送后判断单个消息
                if (sendGroupMsgResp != null && sendGroupMsgResp.hasMessageId()) {
                    sendCount++;
                    log.debug(sendName + " = 发送群消息：" + groupId + "，成功：" + sendGroupMsgResp);
                } else {
                    log.debug(sendName + " = 发送群消息：" + groupId + "，失败：" + sendGroupMsgResp);
                }
                if (rssFeedItem.getGroups().size() - 1 != rssFeedItem.getGroups().indexOf(groupId)) {
                    Thread.sleep(100);
                }
            }
            if (sendCount == rssFeedItem.getGroups().size()) {
                log.info(sendName + " ==>完成发送：" + sendItem.getLink());
            } else {
                log.error(sendName + " = 发送失败：" + sendItem.getLink() + "\n返还消息：" + sendGroupMsgResp + "\n消息内容：" + content);
            }
        }else{
            log.error(sendName+" = 等待Bot5次失败放弃发送："+sendItem.getLink());
        }
    }

    /**
     * 获取图片与视频预览图URL列表
     * @param content 新消息内容
     */
    private void getMedia(String content){
        //获取图片URL列表
        imagesUrl=new ArrayList<>();
        Matcher imagesMatcher=imagePattern.matcher(content);
        while(imagesMatcher.find()){
            imagesUrl.add(imagesMatcher.group(1));
        }
        //获取视频预览URL列表
        videosUrl=new ArrayList<>();
        Matcher videoMatcher=videoPattern.matcher(content);
        while(videoMatcher.find()){
            videosUrl.add(videoMatcher.group(1));
        }
    }

    /**
     * 下载媒体图片
     * @param mediaUrl 媒体Url
     * @return 存储路径
     */
    private String downMedia(String mediaUrl){
        RestTemplate restTemplate=new RestTemplate();
        if(rssFeedItem.isProxy()){
            //设置图片下载代理
            SimpleClientHttpRequestFactory requestFactory=new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(5*1000);
            requestFactory.setReadTimeout(60*1000);
            requestFactory.setProxy(new Proxy(Proxy.Type.HTTP,
                    new InetSocketAddress(
                            (rsshubConfig.getProxyUrl()!=null&&!rsshubConfig.getProxyUrl().isEmpty()?rsshubConfig.getProxyUrl():"127.0.0.1"),
                            rsshubConfig.getProxyPort())));//无代理ip配置默认为127.0.0.1
            restTemplate.setRequestFactory(requestFactory);
        }
        AtomicReference<File> imageFile=new AtomicReference<>();
        try {
            restTemplate.execute(mediaUrl, HttpMethod.GET, null, response -> {
                if (response.getStatusCode() == HttpStatus.OK) {//判断是否响应正常
                    //获取请求文件内容头
                    List<String> strings = response.getHeaders().get("content-type");
                    String imagesSuffix = null;//文件名后缀
                    for (String item : strings) {
                        if (item.matches("image/(.+)")) {
                            //获取图片后缀
                            imagesSuffix = item.substring(6);
                        }
                    }
                    if (imagesSuffix != null) {//请求响应为图片并获取后缀名
                        String fileName = String.valueOf(System.currentTimeMillis());
                        imageFile.set(new File(rsshubConfig.getTempPath() + fileName + "." + imagesSuffix));
                        if (!imageFile.get().getParentFile().exists()) {
                            //若临时文件夹不存在则创建
                            imageFile.get().getParentFile().mkdirs();
                        }
                        FileOutputStream fileOutputStream = new FileOutputStream(imageFile.get());
                        FileCopyUtils.copy(response.getBody(), fileOutputStream);
                        fileOutputStream.close();
                    }
                } else {
                    log.error(sendName + " = 文件下载失败：" + mediaUrl);
                    if (rsshubConfig.isDownloadFailNotify()) {
                        Msg msg = Msg.builder().text("下载").text(mediaUrl).text("时失败");
                        Bot bot = getBot();
                        if (bot != null) {
                            bot.sendPrivateMsg(rsshubConfig.getDownloadFailNotifier(), msg, true);
                        }
                    }
                }
                return response;
            });
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            //抓取中出现网络错误
            log.error(rssFeedItem.getName()+" = 下载媒体图片时网络错误：" + e.getStatusCode() + "\n" + e.getResponseBodyAsString());
        }catch (ResourceAccessException e){
            //抓取中出现网络错误
            log.error(rssFeedItem.getName()+" = 下载媒体图片时网络错误：" + e.getMessage());
        }
        if(imageFile.get()!=null) {
            //若文件成功下载
            return imageFile.get().getAbsolutePath();
        }
        return null;
    }

    /**
     * 翻译文本
     *
     * @return 翻译后的文本
     */
    private String translate(String message) {
        return TranslateService.translate(message, "auto", "zh", translateConfig);
    }

    /**
     * 获取可发送的CoolQ机器人对象
     *
     * @return CoolQ（当前无机器人则返回null
     */
    private Bot getBot() {
        Map<Long, Bot> botMap = botContainer.getBots();
        if (!botMap.isEmpty()) {
            //不为空取出第一个值
            for (Bot bot : botMap.values()) {
                return bot;
            }
        }
        return null;
    }

    /**
     * 下载文件并放入构建图片码
     * @param mediaUrl 媒体图片URL
     * @param message 消息StringBuilder
     */
    private void downAndAdd(String mediaUrl,StringBuilder message){
        String downloadFile=downMedia(mediaUrl);
        if(downloadFile!=null) {
            log.debug(sendName+" = 文件下载到："+downloadFile);
            if(rsshubConfig.getUrlTempAccess()) {
                message.append("[CQ:image,url=").append(rsshubConfig.getLocalUrl()).append(":").append(rsshubConfig.getAccessPort()).append("/image/").append(downloadFile).append("]");
            }else{
                message.append("[CQ:image,file=file:///")
                        .append(downloadFile).append("]");
            }
        }else {
            log.error(sendName+" = 图片下载失败："+mediaUrl);
            message.append("图片下载失败：").append(mediaUrl);
        }
    }
}
