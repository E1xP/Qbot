package com.bot.utils.service;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.utils.service
 * @CLASS_NAME EmailSercice
 * @Description TODO
 * @Date 2023/11/5 0005 下午 9:24
 **/
@Service
public interface EmailService {
    void sendEmail(List<String> sendList, String subject, String content);
}
