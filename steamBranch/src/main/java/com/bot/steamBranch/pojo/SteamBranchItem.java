package com.bot.steamBranch.pojo;

import lombok.Data;

/**
 * @author E1xP@foxmail.com
 * @version 1.0
 * @PACKAGE_NAME com.bot.steamBranch.pojo
 * @CLASS_NAME SteamBranchItem
 * @Description TODO SteamBranch抓取结果
 * @Date 2022/10/22 022 下午 5:40
 **/
@Data
public class SteamBranchItem implements Comparable<SteamBranchItem> {
    /**
     * Branch名称
     */
    String name;
    /**
     * unix时间戳
     */
    Long timeStamp;
    /**
     * 版本Id
     */
    String buildId;
    /**
     * 是否公开分支
     */
    boolean isPublic;

    /**
     * 用于排序
     *
     * @param obj 用于比较的SteamBranchItem类
     * @return 按是否公开、更新时间进行排序
     */
    @Override
    public int compareTo(SteamBranchItem obj) {
        if (this.isPublic() != obj.isPublic) {
            //是否公开
            return this.isPublic ? 1 : -1;
        } else {
            //更新时间
            if (obj.timeStamp == null) {
                return this.timeStamp == null ? 0 : 1;
            }
            return obj.timeStamp.compareTo(this.timeStamp);
        }
    }
}
