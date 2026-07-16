package com.bot.groupModeration.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.bot.entity.CQGroupUser;
import com.bot.groupModeration.config.GroupModerationConfig;
import com.bot.groupModeration.pojo.GroupModerationItem;
import com.bot.groupModeration.pojo.ModerationVerdict;
import com.bot.groupModeration.pojo.TriggerResult;
import com.bot.retdata.ApiData;
import com.bot.retdata.ApiRawData;
import com.bot.retdata.GroupMemberInfoData;
import com.bot.robot.CoolQ;
import com.bot.utils.CQCode;
import com.bot.utils.CQCodeExtend;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 命中后的处置：白名单、撤回、违规提示、禁言、告警群通知。
 * <p>
 * 告警正文发文字消息；违规图通过合并转发节点发送，避免在普通消息里直接 {@code [CQ:image]}。
 */
@Service
@Slf4j
public class ModerationActionService {

    @Resource
    private GroupModerationConfig config;

    private static JSONObject textSegment(String text) {
        JSONObject segment = new JSONObject();
        segment.put("type", "text");
        JSONObject data = new JSONObject();
        data.put("text", text);
        segment.put("data", data);
        return segment;
    }

    private static JSONObject imageSegment(File imageFile) {
        JSONObject segment = new JSONObject();
        segment.put("type", "image");
        JSONObject data = new JSONObject();
        data.put("file", toFileUri(imageFile));
        segment.put("data", data);
        return segment;
    }

    private static String toFileUri(File file) {
        String path = file.getAbsolutePath().replace('\\', '/');
        return path.startsWith("/") ? "file://" + path : "file:///" + path;
    }

    /**
     * 是否跳过审核：仅配置的白名单 QQ。
     */
    public boolean isExempt(GroupModerationItem groupConfig, CQGroupUser sender) {
        if (sender == null) {
            return true;
        }
        List<Long> exempt = groupConfig.getExemptUserIds();
        return exempt != null && exempt.contains(sender.getUserId());
    }

    /**
     * 机器人在该群是否具备禁言权限（admin / owner）
     */
    public boolean botCanBan(CoolQ cq, long groupId) {
        try {
            ApiData<GroupMemberInfoData> info = cq.getGroupMemberInfo(groupId, cq.getSelfId(), false);
            if (info == null || info.getData() == null) {
                return false;
            }
            String role = info.getData().getRole();
            return "admin".equals(role) || "owner".equals(role);
        } catch (Exception e) {
            log.warn("查询机器人群权限失败 groupId={}", groupId, e);
            return false;
        }
    }

    /**
     * 撤回整条群消息（{@code delete_msg}）。
     *
     * @param savedImageName 日志用，持久化相对路径 {@code 群号/文件名}，未保存时为 {@code "-"}
     */
    public boolean recall(CoolQ cq, int messageId, long groupId, long userId, String senderName,
                          TriggerResult trigger, String allScores, String savedImageName) {
        try {
            ApiRawData result = cq.deleteMsg(messageId);
            boolean ok = result != null && result.getRetcode() == 0;
            if (ok) {
                log.info("群审已撤回 groupId={} messageId={} userId={} senderName={} savedImage={} trigger={} score={} scores={} retcode={}",
                        groupId, messageId, userId, senderName, savedImageName, trigger.getLabel(),
                        String.format("%.3f", trigger.getScore()), allScores,
                        result == null ? null : result.getRetcode());
            } else {
                log.warn("群审撤回失败 groupId={} messageId={} userId={} senderName={} savedImage={} trigger={} score={} scores={} retcode={}",
                        groupId, messageId, userId, senderName, savedImageName, trigger.getLabel(),
                        String.format("%.3f", trigger.getScore()), allScores,
                        result == null ? null : result.getRetcode());
            }
            return ok;
        } catch (Exception e) {
            log.warn("群审撤回异常 groupId={} messageId={} userId={} senderName={} savedImage={}",
                    groupId, messageId, userId, senderName, savedImageName, e);
            return false;
        }
    }

    /**
     * 撤回失败时回复原消息，附带精判触发项与检测/聚合信息。
     */
    public void replyRecallFailed(CoolQ cq, long groupId, int messageId,
                                  ModerationVerdict verdict, String savedImageName) {
        replyRefineTip(cq, groupId, messageId, verdict, savedImageName,
                "【群审·精判】消息撤回失败，请管理员手动处理\n",
                "撤回失败已回复精判提醒",
                "撤回失败回复提醒发送失败");
    }

    /**
     * 命中违规图时回复原消息，附带精判触发项与检测/聚合信息（格式同撤回失败提醒）。
     */
    public void replyViolationTip(CoolQ cq, long groupId, int messageId,
                                  ModerationVerdict verdict, String savedImageName) {
        replyRefineTip(cq, groupId, messageId, verdict, savedImageName,
                "【群审·精判】检测到违规图片\n",
                "违规提示已回复",
                "违规提示回复发送失败");
    }

    private void replyRefineTip(CoolQ cq, long groupId, int messageId,
                                ModerationVerdict verdict, String savedImageName,
                                String header, String successLog, String failLog) {
        StringBuilder message = new StringBuilder();
        message.append(CQCodeExtend.reply(messageId));
        message.append(header);
        if (verdict != null) {
            String refineText = verdict.refineActionText(false);
            if (refineText != null && !refineText.isEmpty()) {
                message.append(refineText);
            }
        }
        try {
            cq.sendGroupMsg(groupId, message.toString(), false);
            log.info("{} groupId={} messageId={} savedImage={}", successLog, groupId, messageId, savedImageName);
        } catch (Exception e) {
            log.warn("{} groupId={} messageId={} savedImage={}", failLog, groupId, messageId, savedImageName, e);
        }
    }

    /**
     * 单人禁言（set_group_ban），异步调用，不保证与发图同一时刻
     */
    public void ban(CoolQ cq, long groupId, long userId, long durationSeconds) {
        try {
            ApiRawData result = cq.setGroupBan(groupId, userId, durationSeconds);
            log.info("群审禁言 groupId={} userId={} duration={}s retcode={}",
                    groupId, userId, durationSeconds, result == null ? null : result.getRetcode());
        } catch (Exception e) {
            log.warn("群审禁言失败 groupId={} userId={}", groupId, userId, e);
        }
    }

    public void notifyGroup(CoolQ cq, GroupModerationItem groupConfig, long sourceGroupId,
                            long userId, String senderName, ModerationVerdict verdict,
                            File imageFile, String savedImageName) {
        long targetGroupId = resolveNotifyGroupId(groupConfig);
        if (targetGroupId <= 0) {
            log.warn("群审告警跳过 sourceGroupId={} messageId来源群 userId={} reason=no_notify_target",
                    sourceGroupId, userId);
            return;
        }
        StringBuilder message = new StringBuilder();
        for (Long qq : resolveNotifyAtUserIds(groupConfig)) {
            message.append(CQCode.at(qq));
        }
        message.append("【群审·精判告警】\n");
        message.append("来源群：").append(sourceGroupId).append("\n");
        if (userId > 0) {
            message.append("发送人：").append(senderName != null ? senderName : "")
                    .append("(").append(userId).append(")\n");
        }
        if (verdict != null) {
            String refineText = verdict.refineActionText(true);
            if (refineText != null && !refineText.isEmpty()) {
                message.append(refineText);
            }
        }
        try {
            cq.sendGroupMsg(targetGroupId, message.toString(), false);
            if (imageFile != null && imageFile.isFile()) {
                sendNotifyForward(cq, targetGroupId, userId, senderName, verdict, imageFile);
            }
            TriggerResult trigger = verdict != null ? verdict.getTrigger() : null;
            log.info("群审已告警 sourceGroupId={} targetGroupId={} userId={} senderName={} savedImage={} trigger={} score={}",
                    sourceGroupId, targetGroupId, userId, senderName, savedImageName,
                    trigger == null ? null : trigger.getLabel(),
                    trigger == null ? null : String.format("%.3f", trigger.getScore()));
        } catch (Exception e) {
            log.warn("群审告警失败 sourceGroupId={} targetGroupId={} userId={} savedImage={}",
                    sourceGroupId, targetGroupId, userId, savedImageName, e);
        }
    }

    /**
     * 以合并转发形式发送违规图片及精判详情
     */
    private void sendNotifyForward(CoolQ cq, long targetGroupId, long userId, String senderName,
                                   ModerationVerdict verdict, File imageFile) {
        String forwardSenderName = senderName != null && !senderName.isEmpty() ? senderName : "违规发送者";
        String senderUin = userId > 0 ? String.valueOf(userId) : String.valueOf(cq.getSelfId());

        StringBuilder detail = new StringBuilder("【违规内容·精判】\n");
        if (verdict != null) {
            String refineText = verdict.refineActionText(true);
            if (refineText != null && !refineText.isEmpty()) {
                detail.append(refineText);
            }
        }

        JSONArray content = new JSONArray();
        content.add(textSegment(detail.toString()));
        content.add(imageSegment(imageFile));

        JSONObject data = new JSONObject();
        data.put("name", forwardSenderName);
        data.put("uin", senderUin);
        data.put("content", content);

        JSONObject node = new JSONObject();
        node.put("type", "node");
        node.put("data", data);

        JSONArray messages = new JSONArray();
        messages.add(node);

        ApiData<?> result = cq.sendGroupForwardMsg(targetGroupId, messages);
        log.info("群审告警合并转发 targetGroupId={} userId={} savedImage={} retcode={}",
                targetGroupId, userId, imageFile.getName(),
                result == null ? null : result.getRetcode());
    }

    /**
     * 单群 notify-group-id 优先，否则全局 notify.target-group-id
     */
    private long resolveNotifyGroupId(GroupModerationItem groupConfig) {
        if (groupConfig.getNotifyGroupId() > 0) {
            return groupConfig.getNotifyGroupId();
        }
        if (config.getNotify() != null && config.getNotify().getTargetGroupId() > 0) {
            return config.getNotify().getTargetGroupId();
        }
        return 0;
    }

    /**
     * 群级 notify-at-user-ids 优先，否则回退全局 notify.at-user-ids。
     */
    private List<Long> resolveNotifyAtUserIds(GroupModerationItem groupConfig) {
        List<Long> source = groupConfig.getNotifyAtUserIds();
        if (source == null || source.isEmpty()) {
            if (config.getNotify() != null) {
                source = config.getNotify().getAtUserIds();
            }
        }
        if (source == null || source.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        List<Long> ids = new ArrayList<>();
        for (Long qq : source) {
            if (qq != null && qq > 0) {
                ids.add(qq);
            }
        }
        return ids;
    }

    /**
     * 当前群配置是否应发送告警
     */
    public boolean shouldNotify(GroupModerationItem groupConfig) {
        if (groupConfig.isNotifyEnable()) {
            return resolveNotifyGroupId(groupConfig) > 0;
        }
        return config.getNotify() != null
                && config.getNotify().isEnable()
                && config.getNotify().getTargetGroupId() > 0;
    }
}
