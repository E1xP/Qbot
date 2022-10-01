package com.bot.rsshubqq.pojo;

import lombok.Data;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.rsshubqq.pojo
 * @CLASS_NAME deepLTranslateResult
 * @Description TODO
 * @Date 2022/10/1 001 下午 5:50
 **/
@Data
public class DeeplTranslateResult {
    /**
     * 状态码
     */
    int code;
    /**
     * 错误消息
     */
    String msg;
    /**
     * 翻译结果
     */
    String data;

    DeeplTranslateResult() {
    }
}
