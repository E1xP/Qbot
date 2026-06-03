# utils

各业务模块共享的 **工具与接口** 层，依赖 `spring-cq`，被 `rsshubqq`、`steamBranch`、`groupModeration`、`main` 等引用。

## 组件

| 类 | 说明 |
|----|------|
| `CoolQUtils` | 从 `CQGlobal.robots` 取当前可用的 `CoolQ` 实例（多 Bot 时取第一个），供定时任务发群消息 |
| `CQCodeExtend` | CQ 码扩展：`reply(messageId)`、`poke(qq)` |
| `Time` | 当前时间格式化 |
| `EarlyWarningService` | 告警接口（发送群消息、私聊、邮件、组合告警） |
| `EmailService` | SMTP 发信（由 `main` 的 `EmailConfig` 注入使用） |

## EarlyWarningService

接口定义在 `utils`，**实现在 `main`** 的 `EarlyWarningServiceImpl`：

- `sendEarlyWarning(subject, message)` — 按 `bot.earlyWarning*Enable` 同时走群、私聊、邮件
- `warnOnGroupMessage` / `warnOnPrivateMessage` — 向配置列表发送
- `warnOnEmail` — 调用 `EmailService`

`rsshubqq`、`steamBranch` 在抓取失败、推送失败时注入该接口；`main` 需在运行时扫描到 `EarlyWarningServiceImpl`（`@ComponentScan("com.bot")` 已覆盖）。

## 依赖关系

```
utils → spring-cq
main / rsshubqq / steamBranch / groupModeration → utils
```

## 说明

- 本模块 **无** Spring 配置文件与 CQ 插件。
- 新增跨模块能力时，优先放在 `utils`；若需访问 `BotConfig` 等仅 `main` 持有的配置，可像告警一样采用「接口在 utils、实现在 main」模式。
