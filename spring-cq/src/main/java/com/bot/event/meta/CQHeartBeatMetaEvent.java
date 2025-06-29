package com.bot.event.meta;

import com.alibaba.fastjson.annotation.JSONField;
import com.bot.entity.CQStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 心跳
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CQHeartBeatMetaEvent extends CQMetaEvent {
    /**
     * 状态信息
     */
    @JSONField(name = "status")
    private CQStatus status;

    /**
     * 到下次心跳的间隔，单位毫秒
     */
    @JSONField(name = "interval")
    private Long interval;
}
