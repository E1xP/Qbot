package com.bot.groupModeration.detector;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.bot.groupModeration.config.GroupModerationConfig;
import com.bot.groupModeration.config.OnnxModelLoader;
import com.bot.groupModeration.pojo.NudeNetDetection;
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
 * NudeNet 640m ONNX 推理：YOLO 解码 → {@link NudeNetBanJudgment#refine}。
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

    @Resource
    private GroupModerationConfig config;
    @Resource
    private OnnxModelLoader onnxModelLoader;
    private OrtEnvironment environment;
    private OrtSession session;
    private String inputName;
    private boolean ready;
    private int inputSize;

    private static String normalizeLabel(String label) {
        return label == null ? "" : label.trim().toUpperCase(Locale.ROOT);
    }

    private static String format(float value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    /**
     * 归一化为 {@code [4+nc, anchors]}（channels 为行、anchors 为列）。
     * YOLOv8 常见形状 {@code [1, 22, 8400]} 或 {@code [1, 8400, 22]}。
     */
    private static float[][] extractOutputChannels(Object value) {
        if (value instanceof float[][][]) {
            float[][][] cube = (float[][][]) value;
            if (cube.length == 0 || cube[0].length == 0 || cube[0][0].length == 0) {
                throw new IllegalStateException("NudeNet ONNX 输出为空");
            }
            return orientChannelsFirst(cube[0]);
        }
        if (value instanceof float[][]) {
            float[][] matrix = (float[][]) value;
            if (matrix.length == 0 || matrix[0].length == 0) {
                throw new IllegalStateException("NudeNet ONNX 输出为空");
            }
            return orientChannelsFirst(matrix);
        }
        throw new IllegalStateException("不支持的 NudeNet ONNX 输出类型: " + value.getClass());
    }

    /**
     * 行数较少的一侧为 channel（约 4+18），列数较多的一侧为 anchor（约 8400）。
     */
    private static float[][] orientChannelsFirst(float[][] matrix) {
        if (matrix.length <= matrix[0].length) {
            return matrix;
        }
        return transpose(matrix);
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

    private static List<NudeNetDetection> toDetections(List<RawDetection> raw) {
        List<NudeNetDetection> list = new ArrayList<>(raw.size());
        for (RawDetection d : raw) {
            list.add(new NudeNetDetection(d.getLabel(), d.getScore()));
        }
        return list;
    }

    public NudeNetBanJudgment.RefineResult judge(byte[] imageBytes) throws Exception {
        return NudeNetBanJudgment.refine(detect(imageBytes));
    }

    private List<NudeNetDetection> detect(byte[] imageBytes) throws Exception {
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
        float decodeThreshold = config.getNudenet().getMinDetectionScore();
        try (OnnxTensor tensor = OnnxTensor.createTensor(environment, FloatBuffer.wrap(nchw), shape);
             OrtSession.Result result = session.run(Map.of(inputName, tensor))) {
            float[][] channels = extractOutputChannels(result.get(0).getValue());
            List<RawDetection> raw = decodeYolo(channels, decodeThreshold);
            raw = nonMaxSuppression(raw, config.getNudenet().getNmsIouThreshold());
            return toDetections(raw);
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
