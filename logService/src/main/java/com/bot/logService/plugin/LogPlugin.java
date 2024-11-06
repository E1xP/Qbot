package com.bot.logService.plugin;

import lombok.extern.slf4j.Slf4j;
import net.lz1998.cq.entity.CQFile;
import net.lz1998.cq.entity.CQUser;
import net.lz1998.cq.event.message.CQGroupMessageEvent;
import net.lz1998.cq.event.message.CQPrivateMessageEvent;
import net.lz1998.cq.event.notice.*;
import net.lz1998.cq.event.request.CQFriendRequestEvent;
import net.lz1998.cq.event.request.CQGroupRequestEvent;
import net.lz1998.cq.retdata.GroupInfoData;
import net.lz1998.cq.retdata.GroupMemberInfoData;
import net.lz1998.cq.retdata.LoginInfoData;
import net.lz1998.cq.robot.CQPlugin;
import net.lz1998.cq.robot.CoolQ;
import org.springframework.stereotype.Component;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.logServic
 * @CLASS_NAME LogPlugin
 * @Description TODO 用于记录日志
 * @Date 2024/11/5 0005 下午 10:50
 **/
@Component
@Slf4j
public class LogPlugin extends CQPlugin {

    /**
     * 收到私聊消息
     *
     * @param cq    Coolq实体
     * @param event 事件
     */
    @Override
    public int onPrivateMessage(CoolQ cq, CQPrivateMessageEvent event) {
        StringBuilder info = new StringBuilder();
        info.append(getBotInfo(cq));
        info.append("收到 <- 私聊消息 ");
        //发送者
        CQUser sender = event.getSender();
        info.append("[").append(sender.getNickname()).append("(").append(sender.getUserId()).append(")]: ");
        //消息内容
        info.append(event.getMessage());
        info.append(" (").append(event.getMessageId()).append(")");
        log.info(info.toString());
        return MESSAGE_IGNORE;
    }

    /**
     * 收到群聊消息
     *
     * @param cq    Coolq实体
     * @param event 事件
     */
    @Override
    public int onGroupMessage(CoolQ cq, CQGroupMessageEvent event) {
        StringBuilder info = new StringBuilder();
        info.append(getBotInfo(cq));
        info.append("收到 <- 群聊消息 ");
        //群名称
        long groupId = event.getGroupId();
        info.append(getGroupName(cq, groupId));
        //发送者
        CQUser sender = event.getSender();
        info.append("[").append(sender.getNickname()).append("(").append(sender.getUserId()).append(")]: ");
        //消息内容
        info.append(event.getMessage());
        info.append(" (").append(event.getMessageId()).append(")");
        log.info(info.toString());
        return MESSAGE_IGNORE;
    }

    /**
     * 收到群文件上传
     *
     * @param cq    Coolq实体
     * @param event 事件
     */
    @Override
    public int onGroupUploadNotice(CoolQ cq, CQGroupUploadNoticeEvent event) {
        StringBuilder info = new StringBuilder();
        info.append(getBotInfo(cq));
        info.append("发现 <- 群聊文件 ");
        //群名称
        long groupId = event.getGroupId();
        info.append(getGroupName(cq, groupId));
        //上传人员
        long userId = event.getUserId();
        info.append(getGroupUserInfo(cq, groupId, userId)).append("上传群文件: ");
        //内容
        CQFile file = event.getFile();
        info.append(file.getName());
        info.append(" (").append(file.getId()).append(")");
        log.info(info.toString());
        return MESSAGE_IGNORE;
    }

    /**
     * 群管理员变动
     *
     * @param cq    Coolq实体
     * @param event 事件
     */
    @Override
    public int onGroupAdminNotice(CoolQ cq, CQGroupAdminNoticeEvent event) {
        StringBuilder info = new StringBuilder();
        info.append(getBotInfo(cq));
        info.append("发现 <- 群管理员变动 ");
        //群名称
        long groupId = event.getGroupId();
        info.append(getGroupName(cq, groupId));
        //消息内容
        String subType = event.getSubType();
        switch (subType) {
            case "set": {
                info.append("设置管理员: ");
                break;
            }
            case "unset": {
                info.append("取消管理员: ");
                break;
            }
            default:
                info.append("未知类型[").append(subType).append("]: ");
        }

        long userId = event.getUserId();
        info.append(getGroupUserInfo(cq, groupId, userId));

        log.info(info.toString());
        return MESSAGE_IGNORE;
    }

    @Override
    public int onGroupDecreaseNotice(CoolQ cq, CQGroupDecreaseNoticeEvent event) {
        StringBuilder info = new StringBuilder();
        info.append(getBotInfo(cq));
        info.append("收到 <- 退群: ");
        //群名称
        long groupId = event.getGroupId();
        info.append(getGroupName(cq, groupId));
        //消息内容
        long operatorId = event.getOperatorId();
        long userId = event.getUserId();

        String subType = event.getSubType();
        switch (subType) {
            case "leave": {
                info.append(getGroupUserInfo(cq, groupId, userId)).append("退群");
                break;
            }
            case "kick": {
                info.append(getGroupUserInfo(cq, groupId, userId))
                        .append("被管理员 ")
                        .append(getGroupUserInfo(cq, groupId, operatorId))
                        .append("踢出");
                break;
            }
            case "kick_me": {
                info.append("本号被管理员 ")
                        .append(getGroupUserInfo(cq, groupId, operatorId))
                        .append("踢出群聊");
            }
            default:
                info.append("未知类型[").append(subType).append("] ");
        }

        log.info(info.toString());
        return MESSAGE_IGNORE;
    }

    @Override
    public int onGroupIncreaseNotice(CoolQ cq, CQGroupIncreaseNoticeEvent event) {
        StringBuilder info = new StringBuilder();
        info.append(getBotInfo(cq));
        info.append("收到 <- 加群: ");
        //群名称
        long groupId = event.getGroupId();
        info.append(getGroupName(cq, groupId));
        //消息内容
        long operatorId = event.getOperatorId();
        info.append(getGroupUserInfo(cq, groupId, operatorId));

        String subType = event.getSubType();
        switch (subType) {
            case "approve": {
                info.append("同意 ");
                break;
            }
            case "invite": {
                info.append("邀请 ");
                break;
            }
            default:
                info.append("未知类型[").append(subType).append("] ");
        }

        long userId = event.getUserId();
        info.append(getGroupUserInfo(cq, groupId, userId)).append("入群");

        log.info(info.toString());
        return MESSAGE_IGNORE;
    }

    @Override
    public int onGroupBanNotice(CoolQ cq, CQGroupBanNoticeEvent event) {
        StringBuilder info = new StringBuilder();
        info.append(getBotInfo(cq));
        info.append("发现 <- 群禁言: ");
        //群名称
        long groupId = event.getGroupId();
        info.append(getGroupName(cq, groupId));
        //消息内容
        String subType = event.getSubType();
        long operatorId = event.getOperatorId();
        long userId = event.getUserId();
        long duration = event.getDuration();
        switch (subType) {
            case "ban": {
                if (userId == 0) {
                    info.append("全员禁言");
                } else {
                    info.append(getGroupUserInfo(cq, groupId, userId))
                            .append("被管理员 ")
                            .append(getGroupUserInfo(cq, groupId, operatorId))
                            .append("禁言 ")
                            .append(getFormateTime(duration));
                }
                break;
            }
            case "lift_ban": {
                if (userId == 0) {
                    info.append("解除全员禁言");
                } else {
                    info.append(getGroupUserInfo(cq, groupId, userId))
                            .append("被管理员 ")
                            .append(getGroupUserInfo(cq, groupId, operatorId))
                            .append("解除禁言 ");
                }
                break;
            }
            default:
                info.append("未知类型[").append(subType).append("] ");
        }

        log.info(info.toString());
        return MESSAGE_IGNORE;
    }

    @Override
    public int onFriendAddNotice(CoolQ cq, CQFriendAddNoticeEvent event) {
        String info = getBotInfo(cq) +
                "发现 <- 好友增加：[" +
                //消息内容
                event.getUserId() + "]";
        log.info(info);
        return MESSAGE_IGNORE;
    }

    @Override
    public int onFriendRequest(CoolQ cq, CQFriendRequestEvent event) {
        String info = getBotInfo(cq) +
                "收到 <- 添加好友：[" +
                //消息内容
                event.getUserId() + "]";
        log.info(info);
        return MESSAGE_IGNORE;
    }

    @Override
    public int onGroupRequest(CoolQ cq, CQGroupRequestEvent event) {
        StringBuilder info = new StringBuilder();
        info.append(getBotInfo(cq));
        info.append("收到 <- 加群邀请/请求 ");
        //群名称
        long groupId = event.getGroupId();
        info.append(getGroupName(cq, groupId));
        //消息内容
        String subType = event.getSubType();
        long userId = event.getUserId();
        switch (subType) {
            case "add": {
                info.append("用户: [").append(userId).append("] 请求加入群: [").append(groupId).append("]:").append(event.getComment());
                break;
            }
            case "invite": {
                info.append("用户: [").append(userId).append("] 邀请加入群: [").append(groupId).append("]");
                break;
            }
            default:
                info.append("未知类型[").append(subType).append("]: ");
        }

        log.info(info.toString());
        return MESSAGE_IGNORE;
    }


    /**
     * 获取登陆账号信息
     *
     * @param cq Coolq实体
     * @return 返回登陆账号信息
     */
    private String getBotInfo(CoolQ cq) {
        LoginInfoData data = cq.getLoginInfo().getData();
        return "[" + data.getNickname() + "(" + data.getUser_id() + ")] | ";
    }

    /**
     * 获取群聊用户信息
     *
     * @param cq      Coolq实体
     * @param groupId 群号
     * @param userId  用户id
     * @return 群用户信息
     */
    private String getGroupUserInfo(CoolQ cq, long groupId, long userId) {
        GroupMemberInfoData user = cq.getGroupMemberInfo(groupId, userId, false).getData();
        return "[" + user.getNickname() + "(" + user.getUserId() + ")] ";
    }

    /**
     * 获取群信息
     *
     * @param cq      Coolq实体
     * @param groupId 群号
     * @return 群信息
     */
    private String getGroupName(CoolQ cq, long groupId) {
        GroupInfoData group = cq.getGroupInfo(groupId, false).getData();
        return "[" + group.getGroupName() + "(" + groupId + ")]-";
    }

    /**
     * 获取秒抓换成 天,小时,分钟 的文字
     *
     * @param duration
     * @return
     */
    private String getFormateTime(long duration) {
        long day = duration / (60 * 60 * 24);
        duration = duration % (60 * 60 * 24);
        long hour = duration / (60 * 24);
        duration = duration % (60 * 24);
        long minute = duration / 60;
        duration = duration % 60;
        return (day == 0 ? "" : day + "天") +
                (hour == 0 ? "" : hour + "小时") +
                (minute == 0 ? "" : minute + "分钟") +
                (duration == 0 ? "" : duration + "秒 ");
    }
}
