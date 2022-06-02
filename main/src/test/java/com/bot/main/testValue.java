package com.bot.main;

import com.bot.rsshubqq.pojo.RssFeedItem;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME PACKAGE_NAME
 * @CLASS_NAME valueTest
 * @Description TODO
 * @Date 2022/2/18 下午 10:26
 **/
@SpringBootTest(classes = MainApplication.class)
@RunWith(SpringRunner.class)
public class testValue {

    @Value("${list}")
    List<RssFeedItem> rssList;

    @Test
    void test(){
        System.out.println(rssList);
    }
}
