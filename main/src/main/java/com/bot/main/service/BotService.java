package com.bot.main.service;

import com.bot.utils.Time;
import net.lz1998.pbbot.bot.Bot;
import org.springframework.stereotype.Service;

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

    /**
     * 获取当前Bot运行状态
     *
     * @param bot CqBot
     * @return 运行状态文本
     */
    public String getBotStatus(Bot bot) {
        return "当前登录账号：" + bot.getLoginInfo().getUserId() + "\n" +
                "当前时间：" + Time.getCurrentTime();
    }
}
