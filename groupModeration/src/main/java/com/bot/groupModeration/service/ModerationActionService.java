package com.bot.groupModeration.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.bot.entity.CQGroupUser;
import com.bot.groupModeration.config.GroupModerationConfig;
import com.bot.groupModeration.pojo.GroupModerationItem;
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
import java.util.List;

/**
 * 命中后的处置动作：豁免判断、禁言、撤回、告警群通知。
 * <p>
 * 告警图片通过合并转发节点发送，不在普通消息中直接附带 CQ:image。
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
     * 撤回整条群消息（delete_msg），成功返回 true
     */
    public boolean recall(CoolQ cq, int messageId, long groupId, long userId, String nickname,
                          TriggerResult trigger, String allScores) {
        try {
            ApiRawData result = cq.deleteMsg(messageId);
            boolean ok = result != null && result.getRetcode() == 0;
            if (ok) {
                log.info("群审已撤回 groupId={} messageId={} userId={} nickname={} trigger={} score={} scores={} retcode={}",
                        groupId, messageId, userId, nickname, trigger.getLabel(),
                        String.format("%.3f", trigger.getScore()), allScores,
                        result == null ? null : result.getRetcode());
            } else {
                log.warn("群审撤回失败 groupId={} messageId={} userId={} nickname={} trigger={} score={} scores={} retcode={}",
                        groupId, messageId, userId, nickname, trigger.getLabel(),
                        String.format("%.3f", trigger.getScore()), allScores,
                        result == null ? null : result.getRetcode());
            }
            return ok;
        } catch (Exception e) {
            log.warn("群审撤回异常 groupId={} messageId={} userId={} nickname={}",
                    groupId, messageId, userId, nickname, e);
            return false;
        }
    }

    /**
     * 撤回失败时回复原消息，附带触发项与各分类置信度
     */
    public void replyRecallFailed(CoolQ cq, long groupId, int messageId,
                                  TriggerResult trigger, String allScores) {
        StringBuilder message = new StringBuilder();
        message.append(CQCodeExtend.reply(messageId));
        message.append("【群审】消息撤回失败，请管理员手动处理\n");
        message.append("触发：").append(trigger.getLabel())
                .append("=").append(String.format("%.3f", trigger.getScore()))
                .append(" (阈值").append(trigger.getThreshold()).append(")\n");
        if (allScores != null && !allScores.isEmpty()) {
            message.append("分类置信度：").append(allScores);
        }
        try {
            cq.sendGroupMsg(groupId, message.toString(), false);
            log.info("撤回失败已回复提醒 groupId={} messageId={}", groupId, messageId);
        } catch (Exception e) {
            log.warn("撤回失败回复提醒发送失败 groupId={} messageId={}", groupId, messageId, e);
        }
    }

    /**
     * 单人禁言（set_group_ban），异步调用，不保证与发图同一时刻
     */
    public void ban(CoolQ cq, long groupId, long userId, long durationSeconds) {
        try {
            ApiRawData result = cq.setGroupBan(groupId, userId, durationSeconds);
            log.info("禁言 groupId={} userId={} duration={}s retcode={}",
                    groupId, userId, durationSeconds, result == null ? null : result.getRetcode());
        } catch (Exception e) {
            log.warn("禁言失败 groupId={} userId={}", groupId, userId, e);
        }
    }

    public void notifyGroup(CoolQ cq, GroupModerationItem groupConfig, long sourceGroupId,
                            long userId, String nickname, TriggerResult trigger, String allScores,
                            File imageFile) {
        long targetGroupId = resolveNotifyGroupId(groupConfig);
        if (targetGroupId <= 0) {
            return;
        }
        StringBuilder message = new StringBuilder();
        List<Long> atUsers = groupConfig.getNotifyAtUserIds();
        if (atUsers != null) {
            for (Long qq : atUsers) {
                if (qq != null && qq > 0) {
                    message.append(CQCode.at(qq));
                }
            }
        }
        message.append("【群审告警】\n");
        message.append("来源群：").append(sourceGroupId).append("\n");
        if (userId > 0) {
            message.append("发送人：").append(nickname != null ? nickname : "")
                    .append("(").append(userId).append(")\n");
        }
        message.append("触发：").append(trigger.getLabel())
                .append("=").append(String.format("%.3f", trigger.getScore()))
                .append(" (阈值").append(trigger.getThreshold()).append(")\n");
        message.append("分数：").append(allScores);
        try {
            cq.sendGroupMsg(targetGroupId, message.toString(), false);
            if (imageFile != null && imageFile.isFile()) {
                sendNotifyForward(cq, targetGroupId, userId, nickname, trigger, allScores, imageFile);
            }
            log.info("已通知群 {} 审核告警", targetGroupId);
        } catch (Exception e) {
            log.warn("通知群失败 targetGroupId={}", targetGroupId, e);
        }
    }

    /**
     * 以合并转发形式发送违规图片及分类详情
     */
    private void sendNotifyForward(CoolQ cq, long targetGroupId, long userId, String nickname,
                                   TriggerResult trigger, String allScores, File imageFile) {
        String senderName = nickname != null && !nickname.isEmpty() ? nickname : "违规发送者";
        String senderUin = userId > 0 ? String.valueOf(userId) : String.valueOf(cq.getSelfId());

        JSONArray content = new JSONArray();
        content.add(textSegment("【违规内容】\n"
                + "触发：" + trigger.getLabel() + "=" + String.format("%.3f", trigger.getScore())
                + " (阈值" + trigger.getThreshold() + ")\n"
                + "分类置信度：" + (allScores != null ? allScores : "")));
        content.add(imageSegment(imageFile));

        JSONObject data = new JSONObject();
        data.put("name", senderName);
        data.put("uin", senderUin);
        data.put("content", content);

        JSONObject node = new JSONObject();
        node.put("type", "node");
        node.put("data", data);

        JSONArray messages = new JSONArray();
        messages.add(node);

        ApiData<?> result = cq.sendGroupForwardMsg(targetGroupId, messages);
        log.info("告警合并转发 targetGroupId={} retcode={}",
                targetGroupId, result == null ? null : result.getRetcode());
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
