package com.bot.rsshubqq.service;

import com.alibaba.fastjson.JSONObject;
import com.bot.rsshubqq.config.TranslateConfig;
import com.bot.rsshubqq.pojo.BaiduTranslateResult;
import com.bot.rsshubqq.pojo.DeeplTranslateResult;
import com.bot.rsshubqq.utils.MD5;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
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
    public static String translate(String message, String from, String to, TranslateConfig translateConfig) {
        String apiName = translateConfig.getApiName();
        switch (apiName) {
            case "baidu":
                return baidu(message, from, to, translateConfig);
            case "deepl":
                return deepl(message, from, to, translateConfig);
            default:
                log.error("翻译api配置错误：" + apiName);
        }
        return null;
    }

    private static String baidu(String message, String from, String to, TranslateConfig translateConfig) {
        //构造请求参数
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("q", message);
        params.add("from", from);
        params.add("to", to);
        params.add("appid", translateConfig.getAppId());
        String salt = String.valueOf(System.currentTimeMillis());//获取随机毫秒数作为salt用于签名
        params.add("salt", salt);
        String src = translateConfig.getAppId() + message + salt + translateConfig.getSecurityKey();//签名字符串
        params.add("sign", MD5.md5(src));

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(translateConfig.getUrl());
        URI uri = builder.queryParams(params).build().encode().toUri();
        log.debug("翻译构造的URI为：" + uri);
        RestTemplate restTemplate = getRestTemplate();
        BaiduTranslateResult result = null;
        try {
            result = restTemplate.getForObject(uri, BaiduTranslateResult.class);
        } catch (RestClientException e) {
            log.error("翻译错误:" + e.getMessage());
            return null;
        }
        StringBuilder str = new StringBuilder();
        if (result != null && result.getError_code() == 0) {//
            for (Map<String, String> item : result.getTrans_result()) {
                str.append(item.get("dst"));
                str.append("\n");
            }
        } else {
            log.error("翻译错误:" + result.getError_code());
            return null;
        }
        return new String(str);
    }

    private static String deepl(String message, String from, String to, TranslateConfig translateConfig) {
        //构造请求参数
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        HttpHeaders headers = new HttpHeaders();
        MediaType mediaType = MediaType.parseMediaType("application/json");
        headers.setContentType(mediaType);
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        JSONObject requestMap = new JSONObject();
        message = message.replace("\n", "<br>");
        requestMap.put("text", message);
        requestMap.put("source_lang", from);
        requestMap.put("target_lang", to);
        HttpEntity<JSONObject> httpEntity = new HttpEntity<>(requestMap, headers);

        log.debug("翻译构造的URI为：" + translateConfig.getUrl() + requestMap.toString());
        RestTemplate restTemplate = getRestTemplate();
        DeeplTranslateResult result = null;
        try {
            result = restTemplate.postForObject(translateConfig.getUrl(), httpEntity, DeeplTranslateResult.class);
        } catch (RestClientException e) {
            log.error("翻译错误:" + e.getMessage());
            return null;
        }
        if (result != null && result.getCode() == 200) {
            String data = result.getData();
            data = data.replace("<br>", "\n") + "\n";
            return data;
        } else {
            log.error("翻译错误:" + result.getCode() + " " + result.getMsg());
            return null;
        }
    }

    private static RestTemplate getRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(2 * 1000);
        requestFactory.setReadTimeout(20 * 1000);
        restTemplate.setRequestFactory(requestFactory);
        return restTemplate;
    }
}
