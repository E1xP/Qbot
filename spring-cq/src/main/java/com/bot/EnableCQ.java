package com.bot;

import com.bot.boot.CQAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import({CQAutoConfiguration.class})
public @interface EnableCQ {

}