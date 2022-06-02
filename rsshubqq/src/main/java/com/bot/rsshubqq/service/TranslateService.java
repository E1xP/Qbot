package com.bot.rsshubqq.service;

import com.bot.rsshubqq.config.TranslateConfig;
import com.bot.rsshubqq.pojo.TranslateResult;
import com.bot.rsshubqq.utils.MD5;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.rsshubqq.service
 * @CLASS_NAME TranslateService
 * @Description TODO
 * @Date 2022/2/27 下午 2:20
 **/
@Slf4j
public class TranslateService {
    public static String translate(String message,String from,String to, TranslateConfig translateConfig){
        //构造请求参数
        MultiValueMap<String,String> params=new LinkedMultiValueMap<String,String>();
        params.add("q",message);
        params.add("from",from);
        params.add("to",to);
        params.add("appid",translateConfig.getAppId());
        String salt=String.valueOf(System.currentTimeMillis());//获取随机毫秒数作为salt用于签名
        params.add("salt",salt);
        String src= translateConfig.getAppId()+message+salt+ translateConfig.getSecurityKey();//签名字符串
        params.add("sign", MD5.md5(src));

        UriComponentsBuilder builder=UriComponentsBuilder.fromHttpUrl(translateConfig.getUrl());
        URI uri=builder.queryParams(params).build().encode().toUri();
        log.debug("翻译构造的URI为："+String.valueOf(uri));
        RestTemplate restTemplate = new RestTemplate();
        TranslateResult result = restTemplate.getForObject(uri, TranslateResult.class);
        StringBuilder str=new StringBuilder();
        if(result.getError_code()==0) {//
            for (Map<String, String> item : result.getTrans_result()) {
                str.append(item.get("dst"));
                str.append("\n");
            }
        }else {
            log.error("翻译错误:"+result.getError_code());
            return null;
        }
        return new String(str);
    }
}
