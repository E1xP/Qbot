package com.bot.main.plugin;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import javax.annotation.Resource;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.main.plugin
 * @CLASS_NAME PingPluginTest
 * @Description TODO
 * @Date 2022/2/18 下午 6:45
 **/
@SpringBootTest
class PingPluginTest {
    @Resource
    BaseRespondPlugin pingPlugin;

    @Test
    public void testValue(){
        System.out.println(pingPlugin);
    }
}