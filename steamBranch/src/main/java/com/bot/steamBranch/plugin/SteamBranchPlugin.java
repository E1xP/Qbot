package com.bot.steamBranch.plugin;

import com.bot.event.message.CQGroupMessageEvent;
import com.bot.robot.CQPlugin;
import com.bot.robot.CoolQ;
import com.bot.steamBranch.config.SteamConfig;
import com.bot.steamBranch.mapper.SteamMapper;
import com.bot.steamBranch.pojo.SteamBranchItem;
import com.bot.steamBranch.pojo.SteamFeedItem;
import com.bot.steamBranch.pojo.SteamResult;
import com.bot.utils.CQCodeExtend;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.steamBranch.plugin
 * @CLASS_NAME SteamBranchPlugin
 * @Description TODO 用于应答SteamBranch相关请求
 * @Date 2023/5/2 0002 上午 11:23
 **/
@Component
@Slf4j
public class SteamBranchPlugin extends CQPlugin {

    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yy年MM月dd日 HH:mm:ss");
    @Resource
    SteamConfig steamConfig;
    @Resource
    SteamMapper steamMapper;
    String helpMessage =
            "./steam-List （用于查询支持的游戏列表\n" +
                    "./steam-lastUpdate [游戏Steam ID] (用于查询游戏各分支最近一次更新";

    /**
     * 接收到群消息
     *
     * @param cq    cqBot实体类
     * @param event 群消息事件
     * @return 消息状态
     */
    @Override
    public int onGroupMessage(CoolQ cq, CQGroupMessageEvent event) {
        String comToken = event.getMessage().trim().toLowerCase(Locale.ROOT).split(" ")[0];
        if (event.getMessage().trim().toLowerCase(Locale.ROOT).startsWith("./")) {
            switch (comToken) {
                case "./steam-list":
                    onListGameGroupMessage(cq, event);
                    return MESSAGE_BLOCK;
                case "./steam-lastupdate":
                    onLastUpdateGroupMessage(cq, event);
                    return MESSAGE_BLOCK;
                case "./steam-help":
                    onHelpGroupMessage(cq, event);
                    return MESSAGE_BLOCK;
            }
        }
        return MESSAGE_IGNORE;
    }

    /*收到群聊消息*/

    /**
     * 接收到群-获取游戏列表
     *
     * @param cq    cqBot实体类
     * @param event 群消息事件
     */
    private void onListGameGroupMessage(CoolQ cq, CQGroupMessageEvent event) {
        List<SteamFeedItem> steamList = steamConfig.getSteamList();//支持游戏列表
        StringBuilder strBuilder = new StringBuilder();
        if (steamList != null && !steamList.isEmpty()) {
            strBuilder.append("游戏列表：\n");
            for (SteamFeedItem item : steamList) {//构建游戏列表
                strBuilder
                        .append("- ")
                        .append(item.getAppId())
                        .append(" : ")
                        .append(item.getName())
                        .append("\n");
            }
        } else {
            //无可用的游戏列表
            strBuilder.append("暂无抓取的游戏列表");
        }
        String str = new String(strBuilder);
        cq.sendGroupMsg(event.getGroupId(), str, false);
    }

    /**
     * 接收到群-获取上一次更新情况
     *
     * @param cq    cqBot实体类
     * @param event 群消息事件
     */
    private void onLastUpdateGroupMessage(CoolQ cq, CQGroupMessageEvent event) {
        if (event.getMessage().split(" ").length < 2) {
            //指令格式错误
            String message = "获取游戏更新情况格式错误：./steam-lastUpdate [Steam游戏Id]";
            cq.sendGroupMsg(event.getGroupId(), CQCodeExtend.reply(event.getMessageId()) + message, false);
            return;
        }
        Map<Long, String> gameIdToName = steamConfig.getSteamList().stream().collect(Collectors.toMap(SteamFeedItem::getAppId, SteamFeedItem::getName));//游戏id-游戏名对应Map
        String gameId = event.getMessage().split(" ")[1];//游戏id
        String gameName = null;//游戏名称
        try {
            gameName = gameIdToName.get(Long.parseLong(gameId));
        } catch (NumberFormatException ignored) {
        }
        if (StringUtils.isEmpty(gameName)) {
            //该游戏不在查询列表中
            String message = "游戏Steam Id不存在对应游戏，请参照./steam-List获取可查询游戏id";
            cq.sendGroupMsg(event.getGroupId(), CQCodeExtend.reply(event.getMessageId()) + message, false);
            return;
        }
        SteamResult result = steamMapper.getResult(gameName);//获取分支结果
        if (result == null) {
            //该游戏在查询列表中，但无对应数据
            String message = "该游戏未有对应更新数据，请等待抓取";
            cq.sendGroupMsg(event.getGroupId(), CQCodeExtend.reply(event.getMessageId()) + message, false);
            return;
        }
        List<SteamBranchItem> resultList = result.getSteamBranchItemMap().values().stream()
                .filter(item -> (item.isPublic() || (item.getTimeStamp() != null && System.currentTimeMillis() - 183 * 24 * 60 * 60 * 1000L < item.getTimeStamp() * 1000L)))
                .sorted(SteamBranchItem::compareTo).collect(Collectors.toList());
        StringBuilder strBuilder = new StringBuilder();
        strBuilder
                .append("Steam历史更新结果【")
                .append(result.getName())
                .append("】\n")
                .append("共有")
                .append(result.getSteamBranchItemMap().size())
                .append("个分支\n")
                .append("======================\n(非公开仅最近半年活动分支)\n");
        //构造历史分支结果
        boolean ispublic = false;
        strBuilder.append("= \uD83D\uDCE2公开分支\n");
        for (SteamBranchItem resultItem : resultList) {
            if (ispublic && !resultItem.isPublic()) {
                strBuilder.append("= \uD83D\uDEE0开发分支\n");
            }
            strBuilder.append("  - ").append(resultItem.getName()).append(" :\n")
                    .append("\t版本号：").append(resultItem.getBuildId()).append("\n")
                    .append("\t更新时间：").append(simpleDateFormat.format(new Date(resultItem.getTimeStamp() * 1000))).append("\n");
            ispublic = resultItem.isPublic();
        }
        strBuilder.append("======================");
        String str = new String(strBuilder);
        cq.sendGroupMsg(event.getGroupId(), CQCodeExtend.reply(event.getMessageId()) + str, false);
    }

    /**
     * 接收到群-帮助指令
     *
     * @param cq    cqBot实体类
     * @param event 群消息事件
     */
    private void onHelpGroupMessage(CoolQ cq, CQGroupMessageEvent event) {
        cq.sendGroupMsg(event.getGroupId(), helpMessage, false);
    }

}
