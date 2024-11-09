package com.bot.retdata;

import com.alibaba.fastjson.annotation.JSONField;
import com.bot.entity.CQUser;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.retdata
 * @CLASS_NAME MessageDataGetted
 * @Description TODO 获取消息的返回值
 * @Date 2024/11/7 0007 下午 11:37
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class MessageDataGot extends MessageData {

    /**
     * 是否是群消息
     */
    @JSONField(name = "group")
    private boolean group;
    /**
     * 是群消息时的群号(否则不存在此字段)
     */
    @JSONField(name = "group_id")
    private long groupId;
    /**
     * 消息真实id
     */
    @JSONField(name = "real_id")
    private long realId;
    /**
     * 群消息时为group, 私聊消息为private
     */
    @JSONField(name = "message_type")
    private String messageType;
    /**
     * 发送人信息
     */
    @JSONField(name = "sender")
    private CQUser sender;
    /**
     * 发送时间
     */
    @JSONField(name = "time")
    private long time;
    /**
     * 是群消息时的群号(否则不存在此字段)
     */
    @JSONField(name = "message")
    private String message;
    /**
     * 是群消息时的群号(否则不存在此字段)
     */
    @JSONField(name = "raw_message")
    private String rawMessage;
}
