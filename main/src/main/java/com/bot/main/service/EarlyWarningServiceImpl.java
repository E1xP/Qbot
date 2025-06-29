package com.bot.main.service;

import com.bot.main.config.BotConfig;
import com.bot.robot.CoolQ;
import com.bot.utils.CoolQUtils;
import com.bot.utils.service.EarlyWarningService;
import com.bot.utils.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.main.service
 * @CLASS_NAME EarlyWarningServiceImpl
 * @Description TODO 警告信息发送实现类
 * @Date 2022/12/25 025 下午 7:02
 **/
@Service
@Slf4j
public class EarlyWarningServiceImpl implements EarlyWarningService {

    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM月dd日 HH:mm:ss");
    @Resource
    BotConfig botConfig;

    @Resource
    EmailService emailService;

    /**
     * 发送私聊消息
     *
     * @param message 告警消息
     */
    @Override
    public void warnOnPrivateMessage(String message) {
        CoolQ coolQ = CoolQUtils.getCoolQ();
        if (coolQ == null) {
            log.error("预警信息-获取不到Bot实体");
            return;
        }
        if (botConfig.getEarlyWarningPrivateList().isEmpty()) {
            log.error("预警信息-私聊发送列表为空");
        }
        for (Long privateId : botConfig.getEarlyWarningPrivateList()) {
            coolQ.sendPrivateMsg(privateId, message, false);
        }
    }

    /**
     * 发送群聊消息
     *
     * @param message 告警消息
     */
    @Override
    public void warnOnGroupMessage(String message) {
        CoolQ coolQ = CoolQUtils.getCoolQ();
        if (coolQ == null) {
            log.error("预警信息-获取不到Bot实体");
            return;
        }
        if (botConfig.getEarlyWarningGroupList().isEmpty()) {
            log.error("预警信息-群发送列表为空");
        }
        for (Long groupId : botConfig.getEarlyWarningGroupList()) {
            coolQ.sendGroupMsg(groupId, message, false);
        }
    }

    /**
     * 发送告警消息
     *
     * @param message 告警消息
     */
    @Override
    public void sendEarlyWarning(String subject, String message) {
        Date currentDate = new Date();
        String str = subject +
                "\n----------------------\n" +
                message +
                "\n----------------------\n" +
                "告警时间:" + simpleDateFormat.format(currentDate);
        String sendMessage = str;
        if (botConfig.isEarlyWarningGroupEnable()) {
            warnOnGroupMessage(sendMessage);
        }
        if (botConfig.isEarlyWarningPrivateEnable()) {
            warnOnPrivateMessage(sendMessage);
        }
        if (botConfig.isEarlyWarningEmailEnable()) {
            warnOnEmail(subject, sendMessage);
        }
    }

    /**
     * 发送Email信息
     *
     * @param subject 告警邮件标题
     * @param message 告警消息
     */
    @Override
    public void warnOnEmail(String subject, String message) {
        if (botConfig.isEarlyWarningEmailEnable() && !botConfig.getEarlyWarningEmailList().isEmpty()) {
            log.info("发送告警邮件：{}-{}-{}", botConfig.getEarlyWarningEmailList(), subject, message);
            emailService.sendEmail(botConfig.getEarlyWarningEmailList(), subject, message);
        }
    }
}
