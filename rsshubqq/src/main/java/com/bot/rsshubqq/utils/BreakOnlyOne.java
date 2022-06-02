package com.bot.rsshubqq.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.rsshubqq.utils
 * @CLASS_NAME BreakOnlyOne
 * @Description TODO
 * @Date 2022/3/10 下午 2:53
 **/
public class BreakOnlyOne {
    /**
     * 将字符串中的连续的多个换行缩减成一个换行
     * @param sourceStr  要处理的内容
     * @return	返回的结果
     */
    public static String  multipleLineBreaksKeepOnlyOne(String sourceStr) {
        String result = "";
        if (sourceStr!= null) {
            Pattern p = Pattern.compile("(\r?\n(\\s*\r?\n)+)");//正则表达式
            Matcher m = p.matcher(sourceStr);
            result = m.replaceAll("\r\n");
        }
        return result;
    }
}
