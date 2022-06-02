package com.bot.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.qbot.main.utils
 * @CLASS_NAME Time
 * @Description TODO
 * @Date 2022/2/18 下午 4:07
 **/
public class Time {
    static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    public static String getCurrentTime(String format){
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(format);
        return dateTimeFormatter.format(LocalDateTime.now());
    }
    public static String getCurrentTime(){
        return dateTimeFormatter.format(LocalDateTime.now());
    }
}
