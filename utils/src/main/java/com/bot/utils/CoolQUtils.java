package com.bot.utils;

import net.lz1998.cq.CQGlobal;
import net.lz1998.cq.robot.CoolQ;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.utils
 * @CLASS_NAME CoolQUtils
 * @Description TODO CoolQ工具类
 * @Date 2022/10/25 025 下午 11:56
 **/
public class CoolQUtils {
    /**
     * 获取可发送的CoolQ机器人对象
     *
     * @return CoolQ（当前无机器人则返回null
     */
    public static CoolQ getCoolQ() {
        if (!CQGlobal.robots.values().isEmpty()) {
            //不为空取出第一个值
            for (CoolQ coolQ : CQGlobal.robots.values()) {
                return coolQ;
            }
        }
        return null;
    }
}
