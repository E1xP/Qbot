# groupModeration

群图片双阶段 ONNX 审核：**初筛队列** + **精判队列**。

## 架构

```
Plugin → GroupModerationService
            ├─ prescreenQueue → worker（Inception 初筛）
            └─ refineQueue    → worker（NudeNet 精判）→ 处置
```

| 组件                        | 职责                        |
|---------------------------|---------------------------|
| `GroupModerationService`  | 双队列调度、SHA-256 缓存、撤回/禁言/告警 |
| `GantManOnnxNsfwDetector` | Inception 初筛              |
| `NudeNetOnnxDetector`     | NudeNet 检测 + 聚合 ban 规则    |
| `ModerationActionService` | 命中后处置                     |

## 流程

1. 消息入 **初筛队列** → 拉取图片 → Cache → Inception
2. confidence &lt; 60% → pass，写 Cache
3. confidence ≥ 60% → 入 **精判队列**
4. 精判 worker → NudeNet → ban/pass，写 Cache → 汇总处置

日志：`stage=prescreen|refine`，`refined=true/false`。

## 配置

`application-groupModeration.yml`：`task-queue-capacity`（初筛）、`refine-queue-capacity`（精判）。
