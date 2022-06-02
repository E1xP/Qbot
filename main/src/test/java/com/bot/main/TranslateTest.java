package com.bot.main;

import com.bot.rsshubqq.config.TranslateConfig;
import com.bot.rsshubqq.service.TranslateService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.main
 * @CLASS_NAME TranslateTest
 * @Description TODO
 * @Date 2022/2/27 下午 5:24
 **/
@SpringBootTest(classes = MainApplication.class)
public class TranslateTest {
    @Resource
    TranslateConfig translateConfig;

    @Test
    void translate() {
        String translate = TranslateService.translate("Test\ntest1\ntest2", "auto", "zh", translateConfig);
        System.out.println(translate);
    }
}
