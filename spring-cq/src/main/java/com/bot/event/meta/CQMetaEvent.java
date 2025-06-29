package com.bot.event.meta;

import com.alibaba.fastjson.annotation.JSONField;
import com.bot.event.CQEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class CQMetaEvent extends CQEvent {
    /**
     * heartbeat	元事件类型
     */
    @JSONField(name = "meta_event_type")
    private String metaEventType;
}
