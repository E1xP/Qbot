package com.bot.boot;

import com.bot.robot.CQPlugin;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "spring.cq")
public class CQProperties {
    @Getter
    @Setter
    List<Class<? extends CQPlugin>> pluginList = new ArrayList<>();
    @Getter
    @Setter
    private String url = "/ws/*/";
    @Getter
    @Setter
    private Integer maxTextMessageBufferSize = 512000;
    @Getter
    @Setter
    private Integer maxBinaryMessageBufferSize = 512000;
    @Getter
    @Setter
    private Long maxSessionIdleTimeout = 15 * 60000L;
    @Getter
    @Setter
    private Long apiTimeout = 120000L;

}
