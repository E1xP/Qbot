package com.bot.main.service;

import com.bot.main.config.EmailConfig;
import com.bot.utils.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.main.service
 * @CLASS_NAME EmailService
 * @Description TODO
 * @Date 2023/11/5 0005 下午 9:26
 **/
@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    @Resource
    EmailConfig emailConfig;

    /**
     * 用于发送邮件消息
     *
     * @param sendList 发送列表
     * @param content  发送内容
     */
    @Override
    public void sendEmail(List<String> sendList, String subject, String content) {
        Session session = getSession();
        MimeMessage message = new MimeMessage(session);
        try {
            message.setSubject(subject);
            message.setText(content);
            message.setFrom(emailConfig.getUsername());
            List<InternetAddress> list = new ArrayList<>();
            for (String email : sendList) {
                list.add(new InternetAddress(email));
            }
            message.setRecipients(Message.RecipientType.TO, list.toArray(new InternetAddress[0]));
        } catch (MessagingException e) {
            log.error("邮件发送错误", e);
        }
    }

    public Session getSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host", emailConfig.getSmtpHost());
        props.put("mail.smtp.port", emailConfig.getSmtpPort());
        props.put("mail.smtp.auth", emailConfig.getSmtpAuth());
        props.put("mail.smtp.starttls.enable", emailConfig.getSmtpTls());
        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(emailConfig.getUsername(), emailConfig.getPassword());
            }
        });
    }
}
