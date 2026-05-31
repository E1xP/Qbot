package com.bot.groupModeration.pojo;

import com.bot.groupModeration.util.CqImageParser;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 待审核任务：入队时快照消息 ID 与图片 CQ 参数，消费线程顺序推理后再决定是否撤回。
 */
@Data
@Builder
public class ModerationTask {

    /**
     * 机器人 QQ，用于消费时从 {@link com.bot.CQGlobal} 取 CoolQ
     */
    private long selfId;

    /**
     * 群消息 ID，命中后用于 delete_msg 撤回
     */
    private int messageId;

    private long groupId;
    private long userId;

    /**
     * 发送者昵称（告警文案）
     */
    private String senderNickname;

    /**
     * 入队时解析的图片 CQ 段
     */
    private List<CqImageParser.CqImageSegment> imageSegments;

    /**
     * 入队时间戳（毫秒），便于日志观察排队延迟
     */
    private long enqueuedAt;
}
