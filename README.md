# Qbot

## 简介

基于 [CQHTTP](https://github.com/richardchien/coolq-http-api) / [go-cqhttp](https://github.com/Mrs4s/go-cqhttp) 接入 QQ，采用本仓库内 **spring-cq** 模块（源自 [Spring-CQ](https://github.com/lz1998/spring-cq)）通过反向 WebSocket 与协议端通信，在 Spring Boot 上扩展业务插件的 QQ 机器人。

项目为 Maven 多模块工程，**无需外部数据库**，状态与订阅数据使用本地 JSON 文件持久化。

## 项目结构

| 模块 | 说明 |
|------|------|
| `main` | 启动入口、全局配置、Filter / 基础响应等插件 |
| `spring-cq` | QQ 协议层：WebSocket、事件分发、`CoolQ` API 封装 |
| `rsshubqq` | RssHub RSS 抓取与群推送 |
| `steamBranch` | Steam 游戏分支版本追踪与群推送 |
| `groupModeration` | 群图片双阶段 ONNX 审核（初筛 + 精判） |
| `logService` | 日志相关插件 |
| `utils` | 公共工具（依赖 spring-cq） |

`main` 通过 `application.yml` 的 `spring.profiles.include` 聚合各模块配置：`rsshubqq`、`steamBranch`、`groupModeration`。

## 功能模块

### RssHub 信息抓取推送（`rsshubqq`）

依赖自建或公网的 **[RssHub](https://github.com/DIYgod/RSSHub)** 实例：定时请求各 Feed URL，比对是否有新条目，并按配置推送到指定群聊。支持 HTTP 代理、图片下载、可选翻译（百度 / DeepL 等，经自建翻译接口）。

- 配置：`rsshubqq/src/main/resources/application-rsshubqq.yml`
- 部署：无需额外服务，需配置 JSON 数据库路径（`rsshub.dbPath`）及 RssHub 可访问的 Feed 地址

### Steam 游戏更新追踪（`steamBranch`）

依赖 **[SteamCMD](https://developer.valvesoftware.com/wiki/SteamCMD)** 与 Steam 通信：定时对指定 `appId` 执行 `app_info_print`，解析 branch 信息，检测版本变化后推送到群聊。

- 配置：`steamBranch/src/main/resources/application-steamBranch.yml`
- 部署：本地安装并配置 SteamCMD 路径（`steam.steamCmdPath`），需先完成 Steam 账号登录；配置 JSON 数据库路径（`steam.dbPath`）

### 群图片审核（`groupModeration`）

群消息中的图片经 **初筛队列**（Inception NSFW ONNX）与 **精判队列**（NudeNet 640m ONNX）两阶段本地推理，命中规则后可撤回、禁言并告警。详见 [groupModeration/README.md](groupModeration/README.md)。

```
Plugin → GroupModerationService
            ├─ prescreenQueue → Inception 初筛（GantMan/nsfw_model）
            └─ refineQueue    → NudeNet 精判 → 处置
```

- 配置：`groupModeration/src/main/resources/application-groupModeration.yml`
- 部署：默认 `group-moderation.enable: false`；启用后为 CPU ONNX 推理，无需 GPU；可选外部 ONNX 模型路径

### 其他

- **spring-cq**：插件开发方式见 [spring-cq/README.md](spring-cq/README.md)
- **logService**：运行日志插件，随 `main` 插件列表加载

## 第三方项目与运行时依赖

以下为 Qbot 直接或间接依赖的**外部项目/服务**（部署前请自行安装或部署对应组件）。

| 项目 | 用途 | 链接 |
|------|------|------|
| go-cqhttp / CQHTTP | QQ 协议端（反向 WebSocket） | [go-cqhttp](https://github.com/Mrs4s/go-cqhttp) · [coolq-http-api](https://github.com/richardchien/coolq-http-api) |
| Spring-CQ | 机器人框架（本仓库 `spring-cq` 模块基于此） | [lz1998/spring-cq](https://github.com/lz1998/spring-cq) |
| RssHub | RSS 聚合，提供 Feed URL | [DIYgod/RSSHub](https://github.com/DIYgod/RSSHub) |
| SteamCMD | 查询 Steam App / Branch 信息 | [Valve SteamCMD](https://developer.valvesoftware.com/wiki/SteamCMD) |
| GantMan/nsfw_model | 初筛 ONNX（MobileNet 224 / Inception 299） | [GantMan/nsfw_model](https://github.com/GantMan/nsfw_model) |
| NudeNet | 精判 ONNX（`nudenet_640m` 等） | [notAI-tech/NudeNet](https://github.com/notAI-tech/NudeNet) |
| ONNX Runtime | Java 侧 ONNX 推理引擎 | [microsoft/onnxruntime](https://github.com/microsoft/onnxruntime) |

可选或配置相关：

| 项目 | 用途 | 链接 |
|------|------|------|
| 百度翻译 API | Rss 条目可选翻译（`translate.apiName: baidu`） | [百度翻译开放平台](https://fanyi-api.baidu.com/) |
| DeepL | Rss 条目可选翻译（`translate.apiName: deepl`） | [DeepL API](https://www.deepl.com/pro-api) |
| Rome | RSS/Atom 解析（Maven 依赖） | [rometools/rome](https://github.com/rometools/rome) |
| 酷Q Air + CQHTTP 插件 | 历史方案（spring-cq 文档仍保留） | [酷Q](https://cqp.cc/) · [CQHTTP 插件](https://github.com/richardchien/coolq-http-api/releases) |

## 构建与运行

```bash
# 在项目根目录打包（产物在 main/target）
mvn clean package -pl main -am
```

1. 部署并配置 QQ 协议端（推荐 go-cqhttp），反向 WebSocket 指向本服务，例如 `ws://127.0.0.1:8081/ws/cq/`（端口见 `main/.../application.yml` 的 `server.port`）。
2. 按需修改 `main/src/main/resources/application.yml` 及各模块 `application-*.yml`。
3. 运行打包后的 Spring Boot JAR。

插件加载顺序在 `application.yml` 的 `spring.cq.plugin-list` 中配置（**顺序敏感**：`FilterPlugin` 需在 `GroupModerationPlugin` 之前，以便含图消息正确进入审核流程）。

## 项目部署要点

1. **无外部数据库**：各模块使用 JSON 文件（如 `rsshub.dbPath`、`steam.dbPath`）。
2. **配置入口**：主配置 `main/src/main/resources/application.yml`；模块配置见各 `application-*.yml`（注意文件名拼写为 `application`，非 `applicaiton`）。
3. **RssHub 模块**：配置 Feed 列表、查询间隔、代理与 `dbPath`；确保 RssHub 服务可达。
4. **Steam 模块**：安装 SteamCMD，配置 `steamCmdPath` 与 `dbPath`；仅可查询已登录账号库内相关 App。
5. **群审模块**：设置 `group-moderation.enable` 与 `groups` 群号；内置 ONNX 模型随 JAR 发布，也可通过 `model-path` / `nudenet.model-path` 指定外部文件。

## 模块文档

| 模块 | 文档 |
|------|------|
| `main` | [main/README.md](main/README.md) — 启动、过滤、基础指令、告警实现 |
| `spring-cq` | [spring-cq/README.md](spring-cq/README.md) — 框架与 CQHTTP 对接 |
| `rsshubqq` | [rsshubqq/README.md](rsshubqq/README.md) — RssHub 定时抓取与推送 |
| `steamBranch` | [steamBranch/README.md](steamBranch/README.md) — Steam 分支追踪 |
| `groupModeration` | [groupModeration/README.md](groupModeration/README.md) — 群图片 ONNX 审核 |
| `logService` | [logService/README.md](logService/README.md) — 事件访问日志 |
| `utils` | [utils/README.md](utils/README.md) — 公共工具与告警接口 |
