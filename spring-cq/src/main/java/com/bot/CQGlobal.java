package com.bot;

import com.bot.robot.CoolQ;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CQGlobal {
    public static Map<Long, CoolQ> robots = new ConcurrentHashMap<>();
}
