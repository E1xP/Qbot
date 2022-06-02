package com.bot.main;

import com.bot.main.plugin.BaseRespondPlugin;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class MainApplicationTests {
    @Resource
    BaseRespondPlugin pingPlugin;

    @Test
    void contextLoads() {
        System.out.println(pingPlugin);
    }

}
