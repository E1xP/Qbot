package com.bot.robot;

import com.alibaba.fastjson.JSONObject;
import com.bot.event.enums.*;
import com.bot.event.message.CQDiscussMessageEvent;
import com.bot.event.message.CQGroupMessageEvent;
import com.bot.event.message.CQPrivateMessageEvent;
import com.bot.event.meta.CQHeartBeatMetaEvent;
import com.bot.event.meta.CQLifecycleMetaEvent;
import com.bot.event.notice.*;
import com.bot.event.request.CQFriendRequestEvent;
import com.bot.event.request.CQGroupRequestEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;


/**
 * 事件处理器
 * 先根据 post_type 分类，消息/通知/请求/元事件
 * 然后交给对应的继续分类
 * 职责链模式调用插件，返回MESSAGE_BLOCK停止
 */
@Slf4j
public class EventHandler {

    private final ApplicationContext applicationContext;

    private final CQPlugin defaultPlugin = new CQPlugin();

    public EventHandler(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void handle(CoolQ cq, JSONObject eventJson) {
        PostType postType = PostType.fromValue(eventJson.getString("post_type"));
        if (postType == null) {
            return;
        }
        switch (postType) {
            case MESSAGE:
                handleMessage(cq, eventJson);
                break;
            case NOTICE:
                handleNotice(cq, eventJson);
                break;
            case REQUEST:
                handleRequest(cq, eventJson);
                break;
            case META_EVENT:
                handleMeta(cq, eventJson);
                break;
            case MESSAGE_SENT:
                handleMessageSent(cq, eventJson);
                break;
        }
    }

    private void handleMessageSent(CoolQ cq, JSONObject eventJson) {
        MessageType messageType = MessageType.fromValue(eventJson.getString("message_type"));
        if (messageType == null) {
            return;
        }
        switch (messageType) {
            case PRIVATE: {
                CQPrivateMessageEvent event = eventJson.toJavaObject(CQPrivateMessageEvent.class);
                for (Class<? extends CQPlugin> pluginClass : cq.getPluginList()) {
                    if (getPlugin(pluginClass).onPrivateMessageSent(cq, event) == CQPlugin.MESSAGE_BLOCK)
                        break;
                }
                break;
            }
            case GROUP: {
                CQGroupMessageEvent event = eventJson.toJavaObject(CQGroupMessageEvent.class);
                for (Class<? extends CQPlugin> pluginClass : cq.getPluginList()) {
                    if (getPlugin(pluginClass).onGroupMessageSent(cq, event) == CQPlugin.MESSAGE_BLOCK)
                        break;
                }
                break;
            }
            case DISCUSS: {
                CQDiscussMessageEvent event = eventJson.toJavaObject(CQDiscussMessageEvent.class);
                for (Class<? extends CQPlugin> pluginClass : cq.getPluginList()) {
                    if (getPlugin(pluginClass).onDiscussMessageSent(cq, event) == CQPlugin.MESSAGE_BLOCK)
                        break;
                }
                break;
            }
        }
    }

    private void handleMessage(CoolQ cq, JSONObject eventJson) {
        MessageType messageType = MessageType.fromValue(eventJson.getString("message_type"));
        if (messageType == null) {
            return;
        }
        switch (messageType) {
            case PRIVATE: {
                CQPrivateMessageEvent event = eventJson.toJavaObject(CQPrivateMessageEvent.class);
                for (Class<? extends CQPlugin> pluginClass : cq.getPluginList()) {
                    if (getPlugin(pluginClass).onPrivateMessage(cq, event) == CQPlugin.MESSAGE_BLOCK)
                        break;
                }
                break;
            }
            case GROUP: {
                CQGroupMessageEvent event = eventJson.toJavaObject(CQGroupMessageEvent.class);
                for (Class<? extends CQPlugin> pluginClass : cq.getPluginList()) {
                    if (getPlugin(pluginClass).onGroupMessage(cq, event) == CQPlugin.MESSAGE_BLOCK)
                        break;
                }
                break;
            }
            case DISCUSS: {
                CQDiscussMessageEvent event = eventJson.toJavaObject(CQDiscussMessageEvent.class);
                for (Class<? extends CQPlugin> pluginClass : cq.getPluginList()) {
                    if (getPlugin(pluginClass).onDiscussMessage(cq, event) == CQPlugin.MESSAGE_BLOCK)
                        break;
                }
                break;
            }
        }

    }

    private void handleNotice(CoolQ cq, JSONObject eventJson) {

        NoticeType noticeType = NoticeType.fromValue(eventJson.getString("notice_type"));
        if (noticeType == null) {
            return;
        }

        switch (noticeType) {
            case GROUP_UPLOAD: {
                CQGroupUploadNoticeEvent event = eventJson.toJavaObject(CQGroupUploadNoticeEvent.class);
                for (Class<? extends CQPlugin> pluginClass : cq.getPluginList()) {
                    if (getPlugin(pluginClass).onGroupUploadNotice(cq, event) == CQPlugin.MESSAGE_BLOCK)
                        break;
                }
                break;
            }
            case GROUP_ADMIN: {
                CQGroupAdminNoticeEvent event = eventJson.toJavaObject(CQGroupAdminNoticeEvent.class);
                for (Class<? extends CQPlugin> pluginClass : cq.getPluginList()) {
                    if (getPlugin(pluginClass).onGroupAdminNotice(cq, event) == CQPlugin.MESSAGE_BLOCK)
                        break;
                }
                break;
            }
            case GROUP_DECREASE: {
                CQGroupDecreaseNoticeEvent event = eventJson.toJavaObject(CQGroupDecreaseNoticeEvent.class);
                for (Class<? extends CQPlugin> pluginClass : cq.getPluginList()) {
                    if (getPlugin(pluginClass).onGroupDecreaseNotice(cq, event) == CQPlugin.MESSAGE_BLOCK)
                        break;
                }
                break;
            }
            case GROUP_INCREASE: {
                CQGroupIncreaseNoticeEvent event = eventJson.toJavaObject(CQGroupIncreaseNoticeEvent.class);
                for (Class<? extends CQPlugin> pluginClass : cq.getPluginList()) {
                    if (getPlugin(pluginClass).onGroupIncreaseNotice(cq, event) == CQPlugin.MESSAGE_BLOCK)
                        break;
                }
                break;
            }
            case GROUP_BAN: {
                CQGroupBanNoticeEvent event = eventJson.toJavaObject(CQGroupBanNoticeEvent.class);
                for (Class<? extends CQPlugin> pluginClass : cq.getPluginList()) {
                    if (getPlugin(pluginClass).onGroupBanNotice(cq, event) == CQPlugin.MESSAGE_BLOCK)
                        break;
                }
                break;
            }
            case FRIEND_ADD: {
                CQFriendAddNoticeEvent event = eventJson.toJavaObject(CQFriendAddNoticeEvent.class);
                for (Class<? extends CQPlugin> pluginClass : cq.getPluginList()) {
                    if (getPlugin(pluginClass).onFriendAddNotice(cq, event) == CQPlugin.MESSAGE_BLOCK)
                        break;
                }
                break;
            }
            default:
                break;
        }


    }

    private void handleRequest(CoolQ cq, JSONObject eventJson) {
        RequestType requestType = RequestType.fromValue(eventJson.getString("request_type"));
        if (requestType == null) {
            return;
        }
        switch (requestType) {
            case FRIEND: {
                CQFriendRequestEvent event = eventJson.toJavaObject(CQFriendRequestEvent.class);
                for (Class<? extends CQPlugin> pluginClass : cq.getPluginList()) {
                    if (getPlugin(pluginClass).onFriendRequest(cq, event) == CQPlugin.MESSAGE_BLOCK)
                        break;
                }
                break;
            }
            case GROUP: {
                CQGroupRequestEvent event = eventJson.toJavaObject(CQGroupRequestEvent.class);
                for (Class<? extends CQPlugin> pluginClass : cq.getPluginList()) {
                    if (getPlugin(pluginClass).onGroupRequest(cq, event) == CQPlugin.MESSAGE_BLOCK)
                        break;
                }
                break;
            }
        }
    }

    private void handleMeta(CoolQ cq, JSONObject eventJson) {
        MetaEventType metaType = MetaEventType.fromValue(eventJson.getString("meta_event_type"));
        if (metaType == null) {
            return;
        }
        switch (metaType) {
            case HEARTBEAT: {
                CQHeartBeatMetaEvent event = eventJson.toJavaObject(CQHeartBeatMetaEvent.class);
                for (Class<? extends CQPlugin> pluginClass : cq.getPluginList()) {
                    if (getPlugin(pluginClass).onHeartBeatMeta(cq, event) == CQPlugin.MESSAGE_BLOCK)
                        break;
                }
                break;
            }
            case LIFECYCLE: {
                CQLifecycleMetaEvent event = eventJson.toJavaObject(CQLifecycleMetaEvent.class);
                for (Class<? extends CQPlugin> pluginClass : cq.getPluginList()) {
                    if (getPlugin(pluginClass).onLifecycleMeta(cq, event) == CQPlugin.MESSAGE_BLOCK)
                        break;
                }
                break;
            }
        }
    }

    private CQPlugin getPlugin(Class<? extends CQPlugin> pluginClass) {
        try {
            return applicationContext.getBean(pluginClass);
        } catch (Exception e) {
            log.error("已跳过 {} ，请检查 @Component", pluginClass.getSimpleName());
            return defaultPlugin;
        }
    }
}
