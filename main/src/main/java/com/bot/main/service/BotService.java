package com.bot.main.service;

import com.bot.utils.Time;
import net.lz1998.cq.robot.CoolQ;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.main.service
 * @CLASS_NAME BotService
 * @Description TODO
 * @Date 2022/2/19 下午 5:32
 **/
@Service
public class BotService {

    @Resource
    RestTemplate restTemplate;

    /**
     * 获取当前Bot运行状态
     *
     * @param cq CqBot
     * @return 运行状态文本
     */
    public String getBotStatus(CoolQ cq) {
        return "当前登录账号：" + cq.getLoginInfo().getData().getUser_id() + "\n" +
                "当前HTTP API插件情况：" + (cq.getStatus().getData().isAppGood() ? "正常" : "异常") + "\n" +
                "当前时间：" + Time.getCurrentTime();
    }

    /**
     * 获取公网IP
     *
     * @return 返回当前公网IP
     */
    public String getPublicIp() {
        ResponseEntity<String> forEntity = restTemplate.getForEntity("https://ipinfo.io/ip", String.class);
        if (forEntity.getStatusCode().equals(HttpStatus.OK)) {
            String ip = forEntity.getBody();
            return "当前外网IP出口：" + ip;
        } else {
            return "获取外网出口失败:" + forEntity.getStatusCode().name();
        }
    }
}
