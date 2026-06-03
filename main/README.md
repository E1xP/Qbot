# main

Qbot 的 **启动模块**：Spring Boot 入口、全局 Bot 配置、消息过滤与基础指令、告警实现。

## 职责

| 组件 | 说明 |
|------|------|
| `MainApplication` | 启动类，`@ComponentScan("com.bot")` 扫描全部业务模块 |
| `FilterPlugin` | 消息网关：控制群/私聊是否进入后续插件 |
| `BaseRespondPlugin` | 内置 `./` 指令与加群请求处理 |
| `BotService` | 状态查询、公网 IP 等辅助能力 |
| `EarlyWarningServiceImpl` | `utils.EarlyWarningService` 的实现：群/私聊/邮件告警 |

## 消息过滤（FilterPlugin）

- **私聊**：`bot.replyPrivate=true` 时放行，否则 `MESSAGE_BLOCK`
- **群聊**：
  - 以 `./` 开头的指令 → 放行（供 `BaseRespondPlugin` 等处理）
  - 含图片 CQ 码（`[CQ:image]` 或图片类 `[CQ:file]`）→ 放行（供 `groupModeration`）
  - 其余群消息 → `MESSAGE_BLOCK`

因此插件列表中 **`FilterPlugin` 必须靠前**，且 **`GroupModerationPlugin` 须在其后**（见根 `application.yml` 注释）。

## 群聊指令（BaseRespondPlugin）

| 指令 | 权限 | 说明 |
|------|------|------|
| `./ping` | 所有人 | 回复 Pong（带回复 CQ 码），受 `ping-config` 限速 |
| `./help` | 所有人 | 显示帮助（含 Steam 模块入口说明） |
| `./echo <内容>` | 管理员 | 复读消息 |

## 私聊指令（管理员）

| 指令 | 说明 |
|------|------|
| `./ping` | Pong |
| `./status` | 当前登录账号、协议端版本、时间 |
| `./getpublicip` | 查询公网出口 IP（ipinfo.io） |
| `./send <群号> <内容>` | 向已加入的群发送消息 |
| `./joingroup <群号>` | 将群号加入「允许通过加群邀请」白名单 |

## 加群请求

`onGroupRequest`：仅白名单内群号自动同意，并向首位管理员私聊通知；其余拒绝。

## 配置

主配置：`src/main/resources/application.yml`

| 前缀 | 作用 |
|------|------|
| `bot` | 管理员、是否响应群/私聊、告警开关与目标列表 |
| `ping-config` | `./ping` 限速（`messageCount` × `messageGap`） |
| `server.port` | HTTP/WebSocket 端口（默认 8081） |
| `spring.profiles.include` | 加载 `rsshubqq`、`steamBranch`、`groupModeration` |
| `spring.cq.plugin-list` | CQ 插件加载顺序 |
| `email` | SMTP（告警邮件） |
| `rest-template-config` | HTTP 客户端超时 |

日志：`classpath:logback-spring.xml`（按模块分文件，如 `rsshubqq/`、`steamBranch/`）。

## 构建与运行

```bash
mvn clean package -pl main -am
java -jar main/target/qbot.jar
```

协议端反向 WebSocket 需指向 `ws://<host>:<server.port>/ws/cq/`。
