# groupModeration

群图片双阶段 ONNX 审核：**初筛队列** + **精判队列**。

## 架构

```
Plugin → GroupModerationService
            ├─ prescreenQueue → worker（Inception 初筛）
            └─ refineQueue    → worker（NudeNet 精判）→ 处置
```

| 组件                        | 职责                            |
|---------------------------|-------------------------------|
| `GroupModerationService`  | 双队列调度、SHA-256 缓存、撤回/禁言/告警     |
| `GantManOnnxNsfwDetector` | Inception 初筛                  |
| `NudeNetOnnxDetector`     | NudeNet ONNX 推理（YOLO 解码）      |
| `NudeNetBanJudgment`      | 精判规则（soft-OR 聚合 + 胸/臀联合）与日志文案 |
| `ModerationActionService` | 命中后处置                         |

## NudeNet 分层

```
imageBytes → NudeNetOnnxDetector.judge()
                 └─ detect()           → List<NudeNetDetection>
                 └─ NudeNetBanJudgment.refine() → RefineResult
                        ├─ trigger   是否违规
                        ├─ hitsLog   检测框（中文 + 百分比）
                        └─ aggsLog   聚合分（中文 + 百分比）
```

推理与 ban 规则分离：`NudeNetOnnxDetector` 只负责 ONNX；`NudeNetBanJudgment` 集中维护阈值、聚合算法与日志格式。

## 流程

1. 消息入 **初筛队列** → 拉取图片 → Cache → Inception
2. confidence &lt; 60% → pass，写 Cache
3. confidence ≥ 60% → 入 **精判队列**
4. 精判 worker → NudeNet → ban/pass，写 Cache → 汇总处置

日志示例：

- 初筛：`群审·初筛 | result=放行|过线 | confidence=... | scores=...`
- 精判：`群审·精判 | refineResult=放行|违规 | 触发=胸区 45% | 检测=... | 聚合=...`

## 配置

`application-groupModeration.yml`：`task-queue-capacity`（初筛）、`refine-queue-capacity`（精判）。
