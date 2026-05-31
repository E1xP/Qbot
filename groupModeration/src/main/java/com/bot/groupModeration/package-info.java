/**
 * 群图片双阶段 ONNX 审核模块。
 * <p>
 * <pre>
 * GroupModerationPlugin     消息入口
 * GroupModerationService    初筛队列 + 精判队列 + 缓存 + 处置
 * GantManOnnxNsfwDetector   Inception 初筛
 * NudeNetOnnxDetector       NudeNet 检测与精判规则
 * ModerationActionService   撤回 / 禁言 / 告警
 * </pre>
 */
package com.bot.groupModeration;
