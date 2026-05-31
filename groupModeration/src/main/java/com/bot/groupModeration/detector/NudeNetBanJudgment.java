package com.bot.groupModeration.detector;

import com.bot.groupModeration.pojo.NudeNetDetection;
import com.bot.groupModeration.pojo.TriggerResult;

import java.util.*;

/**
 * NudeNet 精判：检测框 soft-OR 聚合 + 胸/臀联合规则，输出 ban 结论与日志字段。
 * <p>
 * 算法与 {@code com.e1xp.nsfw.NudeNetBanJudgment} 对齐。
 */
public final class NudeNetBanJudgment {

    static final float PRESENCE = 0.25f;
    private static final float TORSO_BAN = 0.38f;
    private static final float BREAST_WEIGHT = 0.52f;
    private static final float BUTTOCKS_WEIGHT = 0.48f;
    private static final Map<String, Rule> RULES = new LinkedHashMap<>();

    static {
        RULES.put("FEMALE_GENITALIA_EXPOSED", new Rule(0.35f));
        RULES.put("MALE_GENITALIA_EXPOSED", new Rule(0.35f));
        RULES.put("ANUS_EXPOSED", new Rule(0.40f));

        RULES.put("FEMALE_BREAST_EXPOSED", new Rule(0.43f, 1.00f));
        RULES.put("BUTTOCKS_EXPOSED", new Rule(0.46f, 1.00f));
        RULES.put("MALE_BREAST_EXPOSED", new Rule(0.65f, 0.60f));
        RULES.put("FEMALE_BREAST_COVERED", new Rule(0.68f, 0.75f));
        RULES.put("BUTTOCKS_COVERED", new Rule(0.72f, 0.75f));

        RULES.put("BELLY_EXPOSED", new Rule(0.85f));
        RULES.put("FEMALE_GENITALIA_COVERED", new Rule(0.72f));
        RULES.put("BELLY_COVERED", new Rule(0.90f));
        RULES.put("ANUS_COVERED", new Rule(0.80f));
        RULES.put("ARMPITS_EXPOSED", new Rule(0.85f));

        RULES.put("FACE_FEMALE", new Rule(-1f));
        RULES.put("FACE_MALE", new Rule(-1f));
        RULES.put("FEET_EXPOSED", new Rule(-1f));
        RULES.put("FEET_COVERED", new Rule(-1f));
        RULES.put("ARMPITS_COVERED", new Rule(-1f));
    }

    private NudeNetBanJudgment() {
    }

    /**
     * 检测 + 规则 + 日志字段，精判唯一入口。
     */
    public static RefineResult refine(List<NudeNetDetection> detections) {
        List<NudeNetDetection> all = detections != null ? detections : new ArrayList<>();

        Evaluation eval = evaluate(all);
        String hitsLog = formatHits(all);
        String aggsLog = formatAggs(eval);
        String summary = buildSummary(all.size(), eval);

        TriggerResult trigger = eval.banned
                ? TriggerResult.of("nudenet:" + eval.banReason, eval.banScore, eval.banThreshold)
                : TriggerResult.notTriggered();
        return new RefineResult(trigger, summary, hitsLog, aggsLog);
    }

    public static String toDisplayTriggerPart(String reason) {
        if (reason == null) {
            return null;
        }
        switch (normalizeLabel(reason)) {
            case "BREAST":
                return "胸区";
            case "BUTTOCKS":
                return "臀区";
            case "TORSO":
                return "胸臀组合";
            default:
                return toDisplayPart(reason);
        }
    }

    private static Evaluation evaluate(List<NudeNetDetection> detections) {
        Map<String, List<Float>> labelScores = new LinkedHashMap<>();
        List<Float> breastWeighted = new ArrayList<>();
        List<Float> buttWeighted = new ArrayList<>();
        float breastRaw = 0f;
        float buttRaw = 0f;

        for (NudeNetDetection detection : detections) {
            if (detection == null || detection.getLabel() == null) {
                continue;
            }
            String label = normalizeLabel(detection.getLabel());
            Rule rule = RULES.get(label);
            if (rule == null || rule.ignored()) {
                continue;
            }

            if (rule.torsoWeight <= 0f) {
                labelScores.computeIfAbsent(label, key -> new ArrayList<>()).add(detection.getScore());
                continue;
            }

            if (isBreast(label)) {
                breastRaw = Math.max(breastRaw, detection.getScore());
                if (detection.getScore() >= PRESENCE) {
                    breastWeighted.add(detection.getScore() * rule.torsoWeight);
                }
            } else {
                buttRaw = Math.max(buttRaw, detection.getScore());
                if (detection.getScore() >= PRESENCE) {
                    buttWeighted.add(detection.getScore() * rule.torsoWeight);
                }
            }
        }

        Map<String, Float> labelAggs = new LinkedHashMap<>();
        for (Map.Entry<String, List<Float>> entry : labelScores.entrySet()) {
            float agg = aggregateScores(entry.getValue());
            if (agg > 0f) {
                labelAggs.put(entry.getKey(), agg);
            }
            Rule rule = RULES.get(entry.getKey());
            if (agg >= rule.threshold) {
                RegionResult region = new RegionResult(breastRaw, buttRaw,
                        aggregateScores(breastWeighted), aggregateScores(buttWeighted));
                return new Evaluation(true, entry.getKey(), agg, rule.threshold, region, labelAggs);
            }
        }

        RegionResult region = new RegionResult(
                breastRaw, buttRaw,
                aggregateScores(breastWeighted), aggregateScores(buttWeighted));
        Evaluation torsoBan = evaluateTorso(region, labelAggs);
        if (torsoBan != null) {
            return torsoBan;
        }
        return new Evaluation(false, null, 0f, 0f, region, labelAggs);
    }

    private static float aggregateScores(List<Float> scores) {
        float combined = 0f;
        if (scores == null) {
            return combined;
        }
        for (float score : scores) {
            if (score < PRESENCE) {
                continue;
            }
            combined += score * (1f - combined);
        }
        return combined;
    }

    private static Evaluation evaluateTorso(RegionResult region, Map<String, Float> labelAggs) {
        boolean hasBreast = region.breastRaw >= PRESENCE;
        boolean hasButt = region.buttRaw >= PRESENCE;
        if (!hasBreast && !hasButt) {
            return null;
        }

        float breastThreshold = RULES.get("FEMALE_BREAST_EXPOSED").threshold;
        float buttThreshold = RULES.get("BUTTOCKS_EXPOSED").threshold;

        if (hasBreast && !hasButt) {
            if (region.breastScore >= breastThreshold) {
                return new Evaluation(true, "BREAST", region.breastScore, breastThreshold, region, labelAggs);
            }
            return null;
        }
        if (hasButt && !hasBreast) {
            if (region.buttScore >= buttThreshold) {
                return new Evaluation(true, "BUTTOCKS", region.buttScore, buttThreshold, region, labelAggs);
            }
            return null;
        }

        float combined = region.breastScore * BREAST_WEIGHT + region.buttScore * BUTTOCKS_WEIGHT;
        if (combined >= TORSO_BAN) {
            return new Evaluation(true, "TORSO", combined, TORSO_BAN, region, labelAggs);
        }
        if (region.breastScore >= breastThreshold) {
            return new Evaluation(true, "BREAST", region.breastScore, breastThreshold, region, labelAggs);
        }
        if (region.buttScore >= buttThreshold) {
            return new Evaluation(true, "BUTTOCKS", region.buttScore, buttThreshold, region, labelAggs);
        }
        return null;
    }

    private static String buildSummary(int boxCount, Evaluation eval) {
        StringBuilder sb = new StringBuilder("boxes=").append(boxCount);
        if (eval.banned) {
            sb.append(", ban=").append(eval.banReason);
        }
        return sb.toString();
    }

    private static String formatHits(List<NudeNetDetection> detections) {
        if (detections.isEmpty()) {
            return null;
        }
        List<NudeNetDetection> sorted = new ArrayList<>(detections);
        sorted.sort((a, b) -> Float.compare(b.getScore(), a.getScore()));
        StringBuilder hits = new StringBuilder();
        int limit = Math.min(6, sorted.size());
        for (int i = 0; i < limit; i++) {
            NudeNetDetection d = sorted.get(i);
            if (i > 0) {
                hits.append('、');
            }
            hits.append(toDisplayPart(d.getLabel())).append(formatPercent(d.getScore()));
        }
        if (sorted.size() > limit) {
            hits.append(" 等").append(sorted.size()).append("处");
        }
        return hits.toString();
    }

    private static String formatAggs(Evaluation eval) {
        StringBuilder aggs = new StringBuilder();
        for (Map.Entry<String, Float> entry : eval.labelAggs.entrySet()) {
            appendAggPart(aggs, toDisplayPart(entry.getKey()), entry.getValue());
        }
        RegionResult region = eval.region;
        if (region.breastScore > 0f) {
            appendAggPart(aggs, "胸区", region.breastScore);
        }
        if (region.buttScore > 0f) {
            appendAggPart(aggs, "臀区", region.buttScore);
        }
        float torso = eval.torsoCombined();
        if (torso >= 0f) {
            appendAggPart(aggs, "胸臀组合", torso);
        }
        return aggs.length() == 0 ? null : aggs.toString();
    }

    private static void appendAggPart(StringBuilder aggs, String name, float value) {
        if (value <= 0f) {
            return;
        }
        if (aggs.length() > 0) {
            aggs.append('、');
        }
        aggs.append(name).append(formatPercent(value));
    }

    private static String toDisplayPart(String label) {
        switch (normalizeLabel(label)) {
            case "FEMALE_GENITALIA_COVERED":
                return "女下体(遮)";
            case "FEMALE_GENITALIA_EXPOSED":
                return "女下体(裸)";
            case "MALE_GENITALIA_EXPOSED":
                return "男下体(裸)";
            case "ANUS_EXPOSED":
                return "肛门(裸)";
            case "ANUS_COVERED":
                return "肛门(遮)";
            case "FEMALE_BREAST_EXPOSED":
                return "女胸(裸)";
            case "FEMALE_BREAST_COVERED":
                return "女胸(遮)";
            case "MALE_BREAST_EXPOSED":
                return "男胸(裸)";
            case "BUTTOCKS_EXPOSED":
                return "臀部(裸)";
            case "BUTTOCKS_COVERED":
                return "臀部(遮)";
            case "BELLY_EXPOSED":
                return "腹部(裸)";
            case "BELLY_COVERED":
                return "腹部(遮)";
            case "ARMPITS_EXPOSED":
                return "腋下(裸)";
            case "ARMPITS_COVERED":
                return "腋下(遮)";
            case "FACE_FEMALE":
                return "女脸";
            case "FACE_MALE":
                return "男脸";
            case "FEET_EXPOSED":
                return "脚(裸)";
            case "FEET_COVERED":
                return "脚(遮)";
            default:
                return label;
        }
    }

    private static String formatPercent(float score) {
        return String.format(Locale.ROOT, "%.0f%%", score * 100f);
    }

    private static boolean isBreast(String label) {
        return label != null && label.contains("BREAST");
    }

    private static String normalizeLabel(String label) {
        return label.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 精判完整输出：触发结果 + 告警摘要 + 日志用检测/聚合文案。
     */
    public static final class RefineResult {
        private final TriggerResult trigger;
        private final String summary;
        private final String hitsLog;
        private final String aggsLog;

        RefineResult(TriggerResult trigger, String summary, String hitsLog, String aggsLog) {
            this.trigger = trigger;
            this.summary = summary;
            this.hitsLog = hitsLog;
            this.aggsLog = aggsLog;
        }

        public TriggerResult getTrigger() {
            return trigger;
        }

        public String getSummary() {
            return summary;
        }

        public String getHitsLog() {
            return hitsLog;
        }

        public String getAggsLog() {
            return aggsLog;
        }
    }

    private static final class Rule {
        final float threshold;
        final float torsoWeight;

        Rule(float threshold) {
            this(threshold, 0f);
        }

        Rule(float threshold, float torsoWeight) {
            this.threshold = threshold;
            this.torsoWeight = torsoWeight;
        }

        boolean ignored() {
            return threshold < 0f;
        }
    }

    private static final class RegionResult {
        final float breastRaw;
        final float buttRaw;
        final float breastScore;
        final float buttScore;

        RegionResult(float breastRaw, float buttRaw, float breastScore, float buttScore) {
            this.breastRaw = breastRaw;
            this.buttRaw = buttRaw;
            this.breastScore = breastScore;
            this.buttScore = buttScore;
        }
    }

    private static final class Evaluation {
        final boolean banned;
        final String banReason;
        final float banScore;
        final float banThreshold;
        final RegionResult region;
        final Map<String, Float> labelAggs;

        Evaluation(boolean banned, String banReason, float banScore, float banThreshold,
                   RegionResult region, Map<String, Float> labelAggs) {
            this.banned = banned;
            this.banReason = banReason;
            this.banScore = banScore;
            this.banThreshold = banThreshold;
            this.region = region;
            this.labelAggs = labelAggs;
        }

        float torsoCombined() {
            if (region.breastRaw < PRESENCE || region.buttRaw < PRESENCE) {
                return -1f;
            }
            return region.breastScore * BREAST_WEIGHT + region.buttScore * BUTTOCKS_WEIGHT;
        }
    }
}
