# rsshubqq

通过 **[RssHub](https://github.com/DIYgod/RSSHub)**（或兼容 RSS/Atom 的 URL）定时拉取 Feed，发现新条目后格式化并推送到指定 QQ 群。

## 架构

```
RsshubScheduleConfig（每 Feed 独立定时器）
        ↓
RssHubController → RssHubService（拉取、比对、持久化）
        ↓ 有新条目
RssHubSendService（组装 CQ 消息、下载图片、可选翻译、群发）
        ↓
RsshubMapper → JSON 文件（dbPath）
```

| 组件 | 职责 |
|------|------|
| `RsshubScheduleConfig` | 为 `rssList` 中每个 Feed 注册 `PeriodicTrigger`，支持 per-feed `queryTime` 与错峰 `itemPauseTime` |
| `RssHubService` | HTTP 拉取 RSS（Rome 解析），与本地状态比对，触发发送线程 |
| `RssHubSendService` | 构建群消息正文；代理下载图片；可选百度/DeepL 翻译 |
| `RsshubMapper` | JSON 读写（已读条目、去重） |
| `RssHubController` | 抓取生命周期、超时线程中断、分支连续失败告警 |
| `MvcConfig` | `urlTempAccess=true` 时映射 `/image/**` → `tempPath` 供 CQ 图片码引用 |

`RssHubPlugin` 仅占位继承 `CQPlugin`，**无交互指令**；能力全部由定时任务驱动。

## 数据流

1. 定时任务启动 `RssHubService` 线程（同名 Feed 未完成则中断旧线程并告警）。
2. 请求 Feed URL（可按 Feed 配置 HTTP 代理）。
3. 解析条目，与 `RsshubMapper` 中记录比较，新条目进入发送队列。
4. `RssHubSendService` 向 `groups` 列表群发；失败时经 `EarlyWarningService` 邮件告警。

## Feed 配置（`RssFeedItem`）

| 字段 | 说明 |
|------|------|
| `name` | 分支名称（日志、告警标识） |
| `url` | RssHub 或任意 RSS URL |
| `groups` | 推送目标群号列表 |
| `translate` | 是否调用翻译接口 |
| `proxy` / `feedProxy` | 拉取/Feed 是否走 HTTP 代理 |
| `twitterRTFilter` / `twitterREFilter` | 推特转发/回复过滤 |
| `queryTime` | 本 Feed 轮询间隔（秒）；`0` 使用全局 `rsshub.query-time` |

## 翻译

`application-rsshubqq.yml` 中 `translate` 段：

- `apiName: baidu` — 百度翻译 API（`appId`、`securityKey`、`url`）
- `apiName: deepl` — DeepL API

## 配置

`src/main/resources/application-rsshubqq.yml`，前缀 `rsshub`：

| 项 | 说明 |
|----|------|
| `enable` | 总开关 |
| `query-time` / `item-pause-time` | 默认轮询间隔、Feed 间启动错峰（秒） |
| `dbPath` | JSON 状态文件路径 |
| `tempPath` | 图片临时目录 |
| `urlTempAccess` / `localUrl` / `accessPort` | 是否通过本机 HTTP 暴露临时图供 CQ 引用 |
| `proxyUrl` / `proxyPort` | 下载代理 |
| `branchErrorInfo` / `branchErrorInfoCount` | 单 Feed 连续抓取失败告警阈值 |
| `rssList` | Feed 列表 |

## 依赖

- 本模块：`rome`（RSS 解析）、`commons-text`、`httpclient`
- 运行时：可访问的 RssHub 实例；`utils`（`CoolQUtils`、`EarlyWarningService`）

## 第三方

- [DIYgod/RSSHub](https://github.com/DIYgod/RSSHub)
- [rometools/rome](https://github.com/rometools/rome)
