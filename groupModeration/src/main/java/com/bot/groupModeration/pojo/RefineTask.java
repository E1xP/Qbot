package com.bot.groupModeration.pojo;

import com.bot.groupModeration.util.CqImageParser;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 精判队列任务：一条消息中需 NudeNet 的图片 + 初筛阶段已完成的图片。
 */
@Data
@Builder
public class RefineTask {

    private long selfId;
    private int messageId;
    private long groupId;
    private long userId;
    /**
     * 群内展示名：群名片优先，无名片时用 QQ 昵称
     */
    private String senderNickname;

    @Builder.Default
    private List<PendingImage> pending = new ArrayList<>();

    @Builder.Default
    private List<DoneImage> prescreenDone = new ArrayList<>();

    @Data
    @Builder
    public static class PendingImage {
        private CqImageParser.CqImageSegment segment;
        private byte[] imageBytes;
        private float prescreenConfidence;
        private String prescreenScores;
    }

    @Data
    public static class DoneImage {
        private final CqImageParser.CqImageSegment segment;
        private final ModerationVerdict verdict;

        public DoneImage(CqImageParser.CqImageSegment segment, ModerationVerdict verdict) {
            this.segment = segment;
            this.verdict = verdict;
        }
    }
}
