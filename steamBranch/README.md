# steamBranch

使用 **[SteamCMD](https://developer.valvesoftware.com/wiki/SteamCMD)** 的 `app_info_print` 定时查询指定游戏的 branch 信息，检测 `buildid` / 时间戳变化后推送到 QQ 群；并提供群聊查询指令。

## 架构

```
SteamScheduleConfig（全局 query-time 定时器）
        ↓
SteamController（顺序调度各 SteamService）
        ↓
SteamService（bash steamcmd.sh + 解析 VDF 风格输出 → JSON）
        ↓ 分支有更新
SteamSendService → 配置的 groupList
        ↓
SteamMapper → JSON（dbPath）
```

| 组件 | 职责 |
|------|------|
| `SteamScheduleConfig` | 按 `steam.query-time` 周期唤醒 `SteamController`；上一轮未结束则挂起/告警 |
| `SteamService` | 执行 SteamCMD，解析 `branches` 块，对比 `SteamResult` 是否变化 |
| `SteamBranchPlugin` | 群聊 `./steam-*` 指令（查询列表与历史） |
| `SteamMapper` | 持久化各游戏各 branch 最近状态 |

## 推送逻辑

- 仅监控配置中的 `branchList`（如 `public`、`supporteralpha`）。
- `buildid` 或更新时间变化时，向 `groupList` 发送更新通知。
- 发送失败时通过 `EarlyWarningService` 发邮件告警。

## 群聊指令（SteamBranchPlugin）

| 指令 | 说明 |
|------|------|
| `./steam-list` | 列出配置中的游戏 `appId` 与名称 |
| `./steam-lastupdate <appId>` | 查询该游戏各 branch 最近版本号与更新时间（公开分支 + 近半年活跃非公开分支） |
| `./steam-help` | 指令帮助 |

指令需以 `./` 开头，且 `FilterPlugin` 已放行此类消息。

## 配置

`src/main/resources/application-steamBranch.yml`，前缀 `steam`：

| 项 | 说明 |
|----|------|
| `enable` | 总开关 |
| `query-time` / `item-pause-time` | 轮询周期、游戏间间隔（秒） |
| `dbPath` | JSON 状态文件 |
| `steamCmdPath` | SteamCMD 启动脚本路径（代码中以 `bash` 调用） |
| `steamUserName` | Steam 登录名（需已在本机 SteamCMD 完成登录） |
| `steamList` | 游戏列表：`name`、`appId`、`branchList`、`groupList` |
| `errorInfo` / `errorInfoCount` | 连续抓取失败告警 |

**注意**：仅能查询当前 Steam 账号库内可见的 App；`steamCmdPath` 在 Linux 下为 shell 脚本，Windows 部署需自行适配调用方式。

## 依赖

- `utils`：`CoolQUtils`、`EarlyWarningService`
- 运行时：已安装并登录的 SteamCMD

## 第三方

- [Valve SteamCMD](https://developer.valvesoftware.com/wiki/SteamCMD)
