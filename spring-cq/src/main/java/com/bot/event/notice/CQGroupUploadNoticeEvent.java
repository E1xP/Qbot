package com.bot.event.notice;

import com.alibaba.fastjson.annotation.JSONField;
import com.bot.entity.CQFile;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 群文件上传
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CQGroupUploadNoticeEvent extends CQNoticeEvent {
    /**
     * 群号
     */
    @JSONField(name = "group_id")
    private long groupId;
    /**
     * 文件信息
     */
    @JSONField(name = "file")
    private CQFile file;
}
