package com.bot.retdata;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

@Data
public class VersionInfoData {

    /**
     * 应用标识, 如 go-cqhttp 固定值
     */
    @JSONField(name = "app_name")
    private String appName;
    /**
     * 应用版本, 如 v0.9.40-fix4
     */
    @JSONField(name = "app_version")
    private String appVersion;
    /**
     * 应用完整名称
     */
    @JSONField(name = "app_full_name")
    private String appFulName;

    @JSONField(name = "runtime_version")
    private String runtimeVersion;

    @JSONField(name = "runtime_os")
    private String runtimeOs;

    @JSONField(name = "version")
    private String version;

    @JSONField(name = "coolq_directory")
    private String coolqDirectory;
    @JSONField(name = "coolq_edition")
    private String coolqEdition;

    @JSONField(name = "plugin_version")
    private String pluginVersion;

    @JSONField(name = "plugin_build_number")
    private long pluginBuildNumber;

    @JSONField(name = "plugin_build_configuration")
    private String plugin_build_configuration;

}
