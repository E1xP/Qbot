package com.bot.utils;

import net.lz1998.cq.utils.CQCode;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.main.service
 * @CLASS_NAME CQCodeUtils
 * @Description TODO 扩展CQ码
 * @Date 2022/9/12 012 下午 10:46
 **/
public class CQCodeExtend extends CQCode {
    /**
     * 构造回复CQCode
     *
     * @param id 回复的消息id
     * @return CQCode
     */
    public static String reply(int id) {
        return "[CQ:reply,id=" + id + "]";
    }

    /**
     * 构造戳一戳CQCode
     *
     * @param id 被戳者qq
     * @return CQCode
     */
    public static String poke(int id) {
        return "[CQ:poke,qq=" + id + "]";
    }

}
