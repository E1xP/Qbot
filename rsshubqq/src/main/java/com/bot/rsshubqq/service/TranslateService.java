package com.bot.rsshubqq.service;

import com.alibaba.fastjson.JSONObject;
import com.bot.rsshubqq.config.TranslateApiName;
import com.bot.rsshubqq.config.TranslateConfig;
import com.bot.rsshubqq.pojo.BaiduTranslateResult;
import com.bot.rsshubqq.pojo.DeeplTranslateResult;
import com.bot.rsshubqq.utils.MD5;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
        List<TranslateConfig.Api> apiList = translateConfig.getApiList();
        if (apiList == null || apiList.isEmpty()) {
            log.error("翻译api列表未配置");
            return null;
        }
        for (int i = 0; i < apiList.size(); i++) {
            TranslateConfig.Api api = apiList.get(i);
            TranslateApiName apiName = api.getApiName();
            log.debug("尝试翻译接口[{}/{}]：{}", i + 1, apiList.size(), apiName);
            String result = null;
            if (apiName == null) {
                log.error("翻译api配置错误：apiName为空");
            } else {
                switch (apiName) {
                    case BAIDU:
                        result = baidu(message, from, to, api);
                        break;
                    case DEEPL_SERVERLESS:
                        result = deeplServerless(message, from, to, api);
                        break;
                    default:
                        log.error("翻译api配置错误：" + apiName);
                }
            }
            if (result != null) {
                return result;
            }
            if (i < apiList.size() - 1) {
                log.warn("翻译接口失败，尝试下一个：{}", apiList.get(i + 1).getApiName());
            }
        }
        log.error("所有翻译接口均失败");
        return null;
    }

    private static String baidu(String message, String from, String to, TranslateConfig.Api api) {
        // 签名用未编码原文；body 按官方 demo 手动 URLEncoder(UTF-8)，避免凭证空白/null 拼接与 form 编码不一致
        String appId = trimToEmpty(api.getAppId());
        String securityKey = trimToEmpty(api.getSecurityKey());
        String url = trimToEmpty(api.getUrl());
        if (appId.isEmpty() || securityKey.isEmpty() || url.isEmpty()) {
            log.error("翻译错误[{}]:appId/securityKey/url未配置或为空", api.getApiName());
            return null;
        }
        if (message == null) {
            message = "";
        }

        String salt = String.valueOf(System.currentTimeMillis());
        String sign = MD5.md5(appId + message + salt + securityKey);
        if (sign == null) {
            log.error("翻译错误[{}]:签名计算失败", api.getApiName());
            return null;
        }

        final String body;
        try {
            body = "q=" + URLEncoder.encode(message, "UTF-8")
                    + "&from=" + URLEncoder.encode(from, "UTF-8")
                    + "&to=" + URLEncoder.encode(to, "UTF-8")
                    + "&appid=" + URLEncoder.encode(appId, "UTF-8")
                    + "&salt=" + URLEncoder.encode(salt, "UTF-8")
                    + "&sign=" + URLEncoder.encode(sign, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error("翻译错误[{}]:{}", api.getApiName(), e.getMessage());
            return null;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "x-www-form-urlencoded", StandardCharsets.UTF_8));
        HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);

        log.debug("翻译POST：{}，appId：{}，q长度：{}", url, appId, message.length());
        BaiduTranslateResult result;
        try {
            result = getRestTemplate().postForObject(url, httpEntity, BaiduTranslateResult.class);
        } catch (RestClientException e) {
            log.error("翻译错误[{}]:{}", api.getApiName(), e.getMessage());
            return null;
        }
        if (result != null && result.getError_code() == 0 && result.getTrans_result() != null) {
            StringBuilder str = new StringBuilder();
            for (Map<String, String> item : result.getTrans_result()) {
                str.append(item.get("dst"));
                str.append("\n");
            }
            return str.toString();
        }
        if (result != null) {
            log.error("翻译错误[{}]:{} {}", api.getApiName(), result.getError_code(), result.getError_msg());
        } else {
            log.error("翻译错误[{}]:空响应", api.getApiName());
        }
        return null;
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String deeplServerless(String message, String from, String to, TranslateConfig.Api api) {
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

        log.debug("翻译构造的URI为：" + api.getUrl() + requestMap);
        RestTemplate restTemplate = getRestTemplate();
        DeeplTranslateResult result;
        try {
            result = restTemplate.postForObject(api.getUrl(), httpEntity, DeeplTranslateResult.class);
        } catch (RestClientException e) {
            log.error("翻译错误[{}]:{}", api.getApiName(), e.getMessage());
            return null;
        }
        if (result != null && result.getData() != null && !result.getData().isEmpty()) {
            String data = result.getData();
            data = data.replace("<br>", "\n") + "\n";
            return data;
        } else if (result != null) {
            log.error("翻译错误[{}]:{} {}", api.getApiName(), result.getCode(), result.getMsg());
        }
        return null;
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
