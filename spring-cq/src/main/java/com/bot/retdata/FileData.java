package com.bot.retdata;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 * get_image / get_record 等接口返回的文件信息。
 */
@Data
public class FileData {
    /**
     * 缓存文件名或路径
     */
    @JSONField(name = "file")
    private String file;
    /**
     * 可下载地址或 file:/// 本地路径；群审模块优先用此字段拉取图片字节。
     */
    @JSONField(name = "url")
    private String url;
    @JSONField(name = "file_size")
    private Long fileSize;
}
