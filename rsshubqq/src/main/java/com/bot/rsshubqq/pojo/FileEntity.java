package com.bot.rsshubqq.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.rsshubqq.pojo
 * @CLASS_NAME FileEntity
 * @Description TODO
 * @Date 2022/9/19 019 下午 10:02
 **/
@Data
@AllArgsConstructor
public class FileEntity {

    private Map<String, RssResult> resultMap;

    public FileEntity() {
        resultMap = new HashMap<>();
    }
}
