package com.bot.groupModeration.detector;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.bot.groupModeration.config.GroupModerationConfig;
import com.bot.groupModeration.config.OnnxModelLoader;
import com.bot.groupModeration.pojo.NudeNetDetection;
import com.bot.groupModeration.pojo.TriggerResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.List;

/**
 * NudeNet 640m YOLOv8 ONNX 检测器。
 * <p>
 * 输入 NCHW {@code [1, 3, 640, 640]}，输出 {@code [1, 4+nc, anchors]}，经 NMS 后返回检测框列表。
 */
@Component
@Slf4j
public class NudeNetOnnxDetector {

    private static final String[] CLASS_NAMES = {
            "FEMALE_GENITALIA_COVERED",
            "FACE_FEMALE",
            "BUTTOCKS_EXPOSED",
            "FEMALE_BREAST_EXPOSED",
            "FEMALE_GENITALIA_EXPOSED",
            "MALE_BREAST_EXPOSED",
            "ANUS_EXPOSED",
            "FEET_EXPOSED",
            "BELLY_COVERED",
            "FEET_COVERED",
            "ARMPITS_COVERED",
            "ARMPITS_EXPOSED",
            "FACE_MALE",
            "BELLY_EXPOSED",
            "MALE_GENITALIA_EXPOSED",
            "ANUS_COVERED",
            "FEMALE_BREAST_COVERED",
            "BUTTOCKS_COVERED"
    };
    private static final float MIN_BOX_SCORE = 0.25f;
    private static final float BREAST_ONLY_THRESHOLD = 0.43f;
    private static final float BUTT_ONLY_THRESHOLD = 0.46f;
    private static final float TORSO_COMBINED_THRESHOLD = 0.38f;
    private static final float BREAST_TORSO_WEIGHT = 0.52f;
    private static final float BUTT_TORSO_WEIGHT = 0.48f;
    private static final Map<String, Float> LABEL_THRESHOLDS = new LinkedHashMap<>();

    static {
        LABEL_THRESHOLDS.put("FEMALE_GENITALIA_EXPOSED", 0.35f);
        LABEL_THRESHOLDS.put("MALE_GENITALIA_EXPOSED", 0.35f);
        LABEL_THRESHOLDS.put("ANUS_EXPOSED", 0.40f);
        LABEL_THRESHOLDS.put("FEMALE_GENITALIA_COVERED", 0.72f);
        LABEL_THRESHOLDS.put("BELLY_EXPOSED", 0.75f);
        LABEL_THRESHOLDS.put("BELLY_COVERED", 0.80f);
        LABEL_THRESHOLDS.put("ANUS_COVERED", 0.80f);
        LABEL_THRESHOLDS.put("ARMPITS_EXPOSED", 0.85f);
    }

    @Resource
    private GroupModerationConfig config;
    @Resource
    private OnnxModelLoader onnxModelLoader;
    private OrtEnvironment environment;
    private OrtSession session;
    private String inputName;
    private boolean ready;
    private int inputSize;

    private static boolean isIgnoredLabel(String label) {
        if (label == null) {
            return true;
        }
        String upper = label.toUpperCase(Locale.ROOT);
        return upper.startsWith("FACE") || upper.startsWith("FEET") || "ARMPITS_COVERED".equals(upper);
    }

    private static boolean isBreastLabel(String label) {
        String upper = normalizeLabel(label);
        return "FEMALE_BREAST_EXPOSED".equals(upper) || "FEMALE_BREAST_COVERED".equals(upper)
                || "MALE_BREAST_EXPOSED".equals(upper);
    }

    private static boolean isButtLabel(String label) {
        String upper = normalizeLabel(label);
        return "BUTTOCKS_EXPOSED".equals(upper) || "BUTTOCKS_COVERED".equals(upper);
    }

    private static float torsoWeight(String label) {
        switch (normalizeLabel(label)) {
            case "FEMALE_BREAST_EXPOSED":
            case "BUTTOCKS_EXPOSED":
                return 1.00f;
            case "FEMALE_BREAST_COVERED":
            case "BUTTOCKS_COVERED":
                return 0.75f;
            case "MALE_BREAST_EXPOSED":
                return 0.60f;
            default:
                return 0f;
        }
    }

    private static boolean hasTorsoDetection(List<NudeNetDetection> valid, boolean breast, boolean butt) {
        for (NudeNetDetection d : valid) {
            if (breast && isBreastLabel(d.getLabel())) {
                return true;
            }
            if (butt && isButtLabel(d.getLabel())) {
                return true;
            }
        }
        return false;
    }

    private static float softOrScores(List<NudeNetDetection> valid, String label) {
        List<Float> scores = new ArrayList<>();
        String target = normalizeLabel(label);
        for (NudeNetDetection d : valid) {
            if (target.equals(normalizeLabel(d.getLabel()))) {
                scores.add(d.getScore());
            }
        }
        return softOr(scores);
    }

    private static float softOrWeighted(List<NudeNetDetection> valid, boolean breast, boolean butt) {
        List<Float> scores = new ArrayList<>();
        for (NudeNetDetection d : valid) {
            if (breast && isBreastLabel(d.getLabel())) {
                scores.add(d.getScore() * torsoWeight(d.getLabel()));
            } else if (butt && isButtLabel(d.getLabel())) {
                scores.add(d.getScore() * torsoWeight(d.getLabel()));
            }
        }
        return softOr(scores);
    }

    private static float softOr(List<Float> scores) {
        if (scores == null || scores.isEmpty()) {
            return 0f;
        }
        double product = 1.0;
        for (float score : scores) {
            float s = Math.max(0f, Math.min(1f, score));
            product *= (1.0 - s);
        }
        return (float) (1.0 - product);
    }

    private static String normalizeLabel(String label) {
        return label == null ? "" : label.trim().toUpperCase(Locale.ROOT);
    }

    private static String format(float value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    /**
     * 兼容 YOLOv8 ONNX 输出：[1, 4+nc, anchors] 或 [1, anchors, 4+nc]。
     */
    private static float[][] extractOutputChannels(Object value) {
        if (value instanceof float[][][]) {
            float[][][] cube = (float[][][]) value;
            if (cube.length == 0 || cube[0].length == 0 || cube[0][0].length == 0) {
                throw new IllegalStateException("NudeNet ONNX 输出为空");
            }
            int dim1 = cube[0].length;
            int dim2 = cube[0][0].length;
            if (dim1 >= dim2) {
                return cube[0];
            }
            return transpose(cube[0]);
        }
        if (value instanceof float[][]) {
            float[][] matrix = (float[][]) value;
            if (matrix.length == 0 || matrix[0].length == 0) {
                throw new IllegalStateException("NudeNet ONNX 输出为空");
            }
            if (matrix.length >= matrix[0].length) {
                return matrix;
            }
            return transpose(matrix);
        }
        throw new IllegalStateException("不支持的 NudeNet ONNX 输出类型: " + value.getClass());
    }

    private static float[][] transpose(float[][] matrix) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        float[][] transposed = new float[cols][rows];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                transposed[c][r] = matrix[r][c];
            }
        }
        return transposed;
    }

    private static float[] toNchw(BufferedImage rgb, int size) {
        float[] nchw = new float[3 * size * size];
        int plane = size * size;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int px = rgb.getRGB(x, y);
                int idx = y * size + x;
                nchw[idx] = ((px >> 16) & 0xFF) / 255f;
                nchw[plane + idx] = ((px >> 8) & 0xFF) / 255f;
                nchw[2 * plane + idx] = (px & 0xFF) / 255f;
            }
        }
        return nchw;
    }

    private static LetterboxResult letterbox(BufferedImage source, int size) {
        int srcW = source.getWidth();
        int srcH = source.getHeight();
        float scale = Math.min((float) size / srcW, (float) size / srcH);
        int newW = Math.round(srcW * scale);
        int newH = Math.round(srcH * scale);
        int padX = (size - newW) / 2;
        int padY = (size - newH) / 2;

        BufferedImage canvas = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        g.setColor(new Color(114, 114, 114));
        g.fillRect(0, 0, size, size);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, padX, padY, newW, newH, null);
        g.dispose();
        return new LetterboxResult(canvas, scale, padX, padY);
    }

    private static List<RawDetection> decodeYolo(float[][] channels, float minScore) {
        List<RawDetection> list = new ArrayList<>();
        int numChannels = channels.length;
        int numAnchors = channels[0].length;
        int numClasses = numChannels - 4;
        if (numClasses <= 0) {
            return list;
        }
        for (int i = 0; i < numAnchors; i++) {
            float cx = channels[0][i];
            float cy = channels[1][i];
            float w = channels[2][i];
            float h = channels[3][i];
            int bestClass = -1;
            float bestScore = 0f;
            for (int c = 0; c < numClasses; c++) {
                float score = channels[4 + c][i];
                if (score > bestScore) {
                    bestScore = score;
                    bestClass = c;
                }
            }
            if (bestClass < 0 || bestScore < minScore) {
                continue;
            }
            String label = bestClass < CLASS_NAMES.length ? CLASS_NAMES[bestClass] : ("CLASS_" + bestClass);
            float x1 = cx - w / 2f;
            float y1 = cy - h / 2f;
            float x2 = cx + w / 2f;
            float y2 = cy + h / 2f;
            list.add(new RawDetection(label, bestScore, x1, y1, x2, y2));
        }
        return list;
    }

    private static List<RawDetection> nonMaxSuppression(List<RawDetection> detections, float iouThreshold) {
        detections.sort(Comparator.comparing(RawDetection::getScore).reversed());
        List<RawDetection> kept = new ArrayList<>();
        boolean[] removed = new boolean[detections.size()];
        for (int i = 0; i < detections.size(); i++) {
            if (removed[i]) {
                continue;
            }
            RawDetection current = detections.get(i);
            kept.add(current);
            for (int j = i + 1; j < detections.size(); j++) {
                if (removed[j]) {
                    continue;
                }
                RawDetection other = detections.get(j);
                if (current.getLabel().equals(other.getLabel()) && iou(current, other) > iouThreshold) {
                    removed[j] = true;
                }
            }
        }
        return kept;
    }

    private static float iou(RawDetection a, RawDetection b) {
        float interX1 = Math.max(a.getX1(), b.getX1());
        float interY1 = Math.max(a.getY1(), b.getY1());
        float interX2 = Math.min(a.getX2(), b.getX2());
        float interY2 = Math.min(a.getY2(), b.getY2());
        float interW = Math.max(0f, interX2 - interX1);
        float interH = Math.max(0f, interY2 - interY1);
        float interArea = interW * interH;
        float areaA = Math.max(0f, a.getX2() - a.getX1()) * Math.max(0f, a.getY2() - a.getY1());
        float areaB = Math.max(0f, b.getX2() - b.getX1()) * Math.max(0f, b.getY2() - b.getY1());
        float union = areaA + areaB - interArea;
        return union <= 0f ? 0f : interArea / union;
    }

    private static List<NudeNetDetection> mapToOriginal(List<RawDetection> raw, LetterboxResult letterbox,
                                                        int srcW, int srcH) {
        List<NudeNetDetection> list = new ArrayList<>();
        for (RawDetection d : raw) {
            float x1 = (d.getX1() - letterbox.padX) / letterbox.scale;
            float y1 = (d.getY1() - letterbox.padY) / letterbox.scale;
            float x2 = (d.getX2() - letterbox.padX) / letterbox.scale;
            float y2 = (d.getY2() - letterbox.padY) / letterbox.scale;
            float nx1 = clamp01(x1 / srcW);
            float ny1 = clamp01(y1 / srcH);
            float nx2 = clamp01(x2 / srcW);
            float ny2 = clamp01(y2 / srcH);
            list.add(new NudeNetDetection(d.getLabel(), d.getScore(), new float[]{nx1, ny1, nx2, ny2}));
        }
        return list;
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    @PostConstruct
    public void init() {
        OnnxModelLoader.ResolvedFileModel resolved = onnxModelLoader.resolveNudeNet(config);
        if (resolved == null) {
            ready = false;
            if (config.getNudenet() != null && config.getNudenet().isEnable()) {
                log.error("NudeNet 模型未加载，精判不可用");
            }
            return;
        }
        File modelFile = resolved.getModelFile();
        inputSize = resolved.getInputSize();
        try {
            environment = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();
            options.setIntraOpNumThreads(Math.max(1, config.getOnnxThreads()));
            session = environment.createSession(modelFile.getAbsolutePath(), options);
            inputName = session.getInputNames().iterator().next();
            ready = true;
            log.info("NudeNet ONNX 已加载 inputSize={} fromClasspath={} path={} inputName={}",
                    inputSize, resolved.isFromClasspath(), modelFile.getAbsolutePath(), inputName);
        } catch (OrtException e) {
            log.error("NudeNet ONNX 模型加载失败: {}", modelFile.getAbsolutePath(), e);
            ready = false;
        }
    }

    @PreDestroy
    public void destroy() {
        if (session != null) {
            try {
                session.close();
            } catch (OrtException e) {
                log.warn("关闭 NudeNet Session 异常", e);
            }
        }
    }

    public boolean isReady() {
        return ready;
    }

    private static String formatHits(List<NudeNetDetection> valid) {
        if (valid.isEmpty()) {
            return null;
        }
        List<NudeNetDetection> sorted = new ArrayList<>(valid);
        sorted.sort(Comparator.comparing(NudeNetDetection::getScore).reversed());
        StringBuilder hits = new StringBuilder();
        int limit = Math.min(8, sorted.size());
        for (int i = 0; i < limit; i++) {
            NudeNetDetection d = sorted.get(i);
            if (i > 0) {
                hits.append(';');
            }
            hits.append(d.getLabel()).append('=').append(format(d.getScore()));
        }
        if (sorted.size() > limit) {
            hits.append(";...(+").append(sorted.size() - limit).append(')');
        }
        return hits.toString();
    }

    private static void appendAgg(StringBuilder aggs, String key, float value) {
        if (value <= 0f) {
            return;
        }
        if (aggs.length() > 0) {
            aggs.append(';');
        }
        aggs.append(key).append('=').append(format(value));
    }

    private static void appendDetectionHits(StringBuilder summary, List<NudeNetDetection> valid) {
        String hits = formatHits(valid);
        if (hits != null) {
            summary.append(", hits=").append(hits);
        }
    }

    /**
     * 检测 + 聚合规则，返回是否 ban 及日志摘要。
     */
    public JudgeResult judge(byte[] imageBytes) throws Exception {
        return judge(predict(imageBytes));
    }

    public List<NudeNetDetection> predict(byte[] imageBytes) throws Exception {
        if (!ready) {
            throw new IllegalStateException("NudeNet 模型未就绪");
        }
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (source == null) {
            return new ArrayList<>();
        }
        LetterboxResult letterbox = letterbox(source, inputSize);
        float[] nchw = toNchw(letterbox.image, inputSize);
        long[] shape = new long[]{1, 3, inputSize, inputSize};
        float decodeThreshold = Math.max(MIN_BOX_SCORE, config.getNudenet().getMinDetectionScore());
        try (OnnxTensor tensor = OnnxTensor.createTensor(environment, FloatBuffer.wrap(nchw), shape);
             OrtSession.Result result = session.run(Map.of(inputName, tensor))) {
            float[][] channels = extractOutputChannels(result.get(0).getValue());
            List<RawDetection> raw = decodeYolo(channels, decodeThreshold);
            raw = nonMaxSuppression(raw, config.getNudenet().getNmsIouThreshold());
            return mapToOriginal(raw, letterbox, source.getWidth(), source.getHeight());
        }
    }

    public JudgeResult judge(List<NudeNetDetection> detections) {
        List<NudeNetDetection> valid = new ArrayList<>();
        if (detections != null) {
            for (NudeNetDetection d : detections) {
                if (d != null && d.getScore() >= MIN_BOX_SCORE && !isIgnoredLabel(d.getLabel())) {
                    valid.add(d);
                }
            }
        }

        String hits = formatHits(valid);
        StringBuilder aggs = new StringBuilder();
        StringBuilder summary = new StringBuilder();
        summary.append("boxes=").append(valid.size());
        appendDetectionHits(summary, valid);

        for (Map.Entry<String, Float> entry : LABEL_THRESHOLDS.entrySet()) {
            float agg = softOrScores(valid, entry.getKey());
            if (agg > 0f) {
                appendAgg(aggs, entry.getKey() + "Agg", agg);
                summary.append(", ").append(entry.getKey()).append("Agg=").append(format(agg));
            }
            if (agg >= entry.getValue()) {
                summary.append(", ban=label");
                return JudgeResult.ban(entry.getKey(), agg, entry.getValue(), summary.toString(), hits, aggs.toString());
            }
        }

        boolean hasBreast = hasTorsoDetection(valid, true, false);
        boolean hasButt = hasTorsoDetection(valid, false, true);
        float breastAgg = softOrWeighted(valid, true, false);
        float buttAgg = softOrWeighted(valid, false, true);
        if (breastAgg > 0f) {
            appendAgg(aggs, "breastAgg", breastAgg);
            summary.append(", breastAgg=").append(format(breastAgg));
        }
        if (buttAgg > 0f) {
            appendAgg(aggs, "buttAgg", buttAgg);
            summary.append(", buttAgg=").append(format(buttAgg));
        }

        if (hasBreast && !hasButt) {
            if (breastAgg >= BREAST_ONLY_THRESHOLD) {
                summary.append(", ban=breast_only");
                return JudgeResult.ban("BREAST", breastAgg, BREAST_ONLY_THRESHOLD, summary.toString(), hits, aggs.toString());
            }
        } else if (hasButt && !hasBreast) {
            if (buttAgg >= BUTT_ONLY_THRESHOLD) {
                summary.append(", ban=butt_only");
                return JudgeResult.ban("BUTTOCKS", buttAgg, BUTT_ONLY_THRESHOLD, summary.toString(), hits, aggs.toString());
            }
        } else if (hasBreast && hasButt) {
            float torso = breastAgg * BREAST_TORSO_WEIGHT + buttAgg * BUTT_TORSO_WEIGHT;
            appendAgg(aggs, "torso", torso);
            summary.append(", torso=").append(format(torso));
            if (torso >= TORSO_COMBINED_THRESHOLD) {
                summary.append(", ban=torso");
                return JudgeResult.ban("TORSO", torso, TORSO_COMBINED_THRESHOLD, summary.toString(), hits, aggs.toString());
            }
            if (breastAgg >= BREAST_ONLY_THRESHOLD) {
                summary.append(", ban=breast_combo");
                return JudgeResult.ban("BREAST", breastAgg, BREAST_ONLY_THRESHOLD, summary.toString(), hits, aggs.toString());
            }
            if (buttAgg >= BUTT_ONLY_THRESHOLD) {
                summary.append(", ban=butt_combo");
                return JudgeResult.ban("BUTTOCKS", buttAgg, BUTT_ONLY_THRESHOLD, summary.toString(), hits, aggs.toString());
            }
        }

        return JudgeResult.pass(summary.toString(), hits, aggs.toString());
    }

    public static final class JudgeResult {
        private final TriggerResult trigger;
        private final String summary;
        private final String hits;
        private final String aggs;

        private JudgeResult(TriggerResult trigger, String summary, String hits, String aggs) {
            this.trigger = trigger;
            this.summary = summary;
            this.hits = hits;
            this.aggs = aggs;
        }

        static JudgeResult pass(String summary, String hits, String aggs) {
            return new JudgeResult(TriggerResult.notTriggered(), summary, hits, aggs);
        }

        static JudgeResult ban(String reason, float score, float threshold, String summary, String hits, String aggs) {
            return new JudgeResult(TriggerResult.of("nudenet:" + reason, score, threshold), summary, hits, aggs);
        }

        public TriggerResult getTrigger() {
            return trigger;
        }

        public String getSummary() {
            return summary;
        }

        public String getHits() {
            return hits;
        }

        public String getAggs() {
            return aggs;
        }
    }

    private static class LetterboxResult {
        final BufferedImage image;
        final float scale;
        final int padX;
        final int padY;

        LetterboxResult(BufferedImage image, float scale, int padX, int padY) {
            this.image = image;
            this.scale = scale;
            this.padX = padX;
            this.padY = padY;
        }
    }

    private static class RawDetection {
        private final String label;
        private final float score;
        private final float x1;
        private final float y1;
        private final float x2;
        private final float y2;

        RawDetection(String label, float score, float x1, float y1, float x2, float y2) {
            this.label = label;
            this.score = score;
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        String getLabel() {
            return label;
        }

        float getScore() {
            return score;
        }

        float getX1() {
            return x1;
        }

        float getY1() {
            return y1;
        }

        float getX2() {
            return x2;
        }

        float getY2() {
            return y2;
        }
    }
}
