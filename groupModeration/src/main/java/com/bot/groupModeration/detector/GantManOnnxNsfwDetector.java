package com.bot.groupModeration.detector;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.bot.groupModeration.config.GroupModerationConfig;
import com.bot.groupModeration.config.OnnxModelLoader;
import com.bot.groupModeration.pojo.NsfwPrediction;
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
import java.util.Map;

/**
 * 基于 ONNX Runtime 的 GantMan/nsfw_model 五分类推理。
 * <p>
 * 模型：内置 classpath（{@code model-variant: 224|299}）或外部 {@code model-path}。
 * 输入 NHWC {@code [1, H, H, 3]}，RGB 像素归一化至 [0,1]。
 * 输出五类概率见 {@link NsfwPrediction#LABELS}，初筛置信度见 {@link NsfwPrediction#getConfidencePercent()}。
 */
@Component
@Slf4j
public class GantManOnnxNsfwDetector {

    @Resource
    private GroupModerationConfig config;
    @Resource
    private OnnxModelLoader onnxModelLoader;

    private OrtEnvironment environment;
    private OrtSession session;
    private String inputName;
    /**
     * 模型与会话是否可用；false 时 {@link com.bot.groupModeration.service.GroupModerationService} 不会入队。
     */
    private boolean ready;
    private int inputSize;

    /**
     * 缩放至 size×size，RGB 通道，数值归一化到 [0,1]，按 NHWC 展平。
     */
    private static float[] preprocess(BufferedImage source, int size) {
        BufferedImage rgb = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        rgb.getGraphics().drawImage(source.getScaledInstance(size, size, Image.SCALE_SMOOTH), 0, 0, null);
        float[] nhwc = new float[size * size * 3];
        int i = 0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int px = rgb.getRGB(x, y);
                nhwc[i++] = ((px >> 16) & 0xFF) / 255f;
                nhwc[i++] = ((px >> 8) & 0xFF) / 255f;
                nhwc[i++] = (px & 0xFF) / 255f;
            }
        }
        return nhwc;
    }

    /**
     * 兼容不同导出脚本的输出维度（float[][] 或 float[]）
     */
    private static float[] flattenOutput(Object value) {
        if (value instanceof float[][]) {
            return ((float[][]) value)[0];
        }
        if (value instanceof float[]) {
            return (float[]) value;
        }
        if (value instanceof double[][]) {
            double[] row = ((double[][]) value)[0];
            float[] out = new float[row.length];
            for (int i = 0; i < row.length; i++) {
                out[i] = (float) row[i];
            }
            return out;
        }
        throw new IllegalStateException("不支持的 ONNX 输出类型: " + value.getClass());
    }

    @PostConstruct
    public void init() {
        OnnxModelLoader.ResolvedModel resolved = onnxModelLoader.resolve(config);
        if (resolved == null) {
            ready = false;
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
            log.info("群审 ONNX 已加载 variant={} inputSize={} fromClasspath={} path={} inputName={}",
                    resolved.getVariant().name(), inputSize, resolved.isFromClasspath(),
                    modelFile.getAbsolutePath(), inputName);
        } catch (OrtException e) {
            log.error("群审 ONNX 模型加载失败: {}", modelFile.getAbsolutePath(), e);
            ready = false;
        }
    }

    @PreDestroy
    public void destroy() {
        if (session != null) {
            try {
                session.close();
            } catch (OrtException e) {
                log.warn("关闭 ONNX Session 异常", e);
            }
        }
    }

    public boolean isReady() {
        return ready;
    }

    public NsfwPrediction predict(byte[] imageBytes) throws Exception {
        if (!ready) {
            throw new IllegalStateException("ONNX 模型未就绪");
        }
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (source == null) {
            throw new IllegalArgumentException("无法解码图片");
        }
        float[] nhwc = preprocess(source, inputSize);
        long[] shape = new long[]{1, inputSize, inputSize, 3};
        try (OnnxTensor tensor = OnnxTensor.createTensor(environment, FloatBuffer.wrap(nhwc), shape);
             OrtSession.Result result = session.run(Map.of(inputName, tensor))) {
            Object value = result.get(0).getValue();
            float[] probs = flattenOutput(value);
            NsfwPrediction prediction = new NsfwPrediction();
            int labelCount = Math.min(probs.length, NsfwPrediction.LABELS.length);
            for (int i = 0; i < labelCount; i++) {
                prediction.getScores().put(NsfwPrediction.LABELS[i], probs[i]);
            }
            return prediction;
        }
    }
}
