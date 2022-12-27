package com.bot.utils.service;

import org.springframework.stereotype.Service;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.utils
 * @CLASS_NAME EmailSendService
 * @Description TODO 警告服务发送信息接口类（具体在main包中实现
 * @Date 2022/12/15 015 下午 10:25
 **/
@Service
public interface EarlyWarningService {

    void warnOnPrivateMessage(String message);

    void warnOnGroupMessage(String message);

    void sendEarlyWarning(String message);

}
