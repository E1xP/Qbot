package com.bot.rsshubqq.pojo;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.rsshubqq.pojo
 * @CLASS_NAME TranslateResult
 * @Description TODO 百度翻译结果类
 * @Date 2022/2/27 下午 3:33
 **/
@Data
public class BaiduTranslateResult {
    String from;
    String to;
    int error_code = 0;
    ArrayList<HashMap<String, String>> trans_result;

    public BaiduTranslateResult() {
    }
}
