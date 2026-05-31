package com.bot.groupModeration.config;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * 解析 ONNX 模型文件：优先外部 model-path，否则从 classpath 内置资源解压到临时文件供 ONNX Runtime 加载。
 */
@Component
@Slf4j
public class OnnxModelLoader {

    /**
     * 解析 ONNX 模型：外部 {@code model-path} 优先，否则从 classpath 解压到临时文件。
     *
     * @return 不可用（文件缺失或解压失败）时返回 null
     */
    public ResolvedModel resolve(GroupModerationConfig config) {
        OnnxModelVariant variant = OnnxModelVariant.from(config.getModelVariant());
        int inputSize = config.getModelInputSize() > 0 ? config.getModelInputSize() : variant.getInputSize();

        if (StringUtils.hasText(config.getModelPath())) {
            File external = new File(config.getModelPath());
            if (external.isFile()) {
                return new ResolvedModel(external, inputSize, variant, false);
            }
            log.warn("配置的 model-path 不存在，将尝试内置模型: {}", external.getAbsolutePath());
        }

        try {
            File classpathModel = materializeClasspathModel(variant);
            return new ResolvedModel(classpathModel, inputSize, variant, true);
        } catch (Exception e) {
            log.error("加载内置 ONNX 失败 variant={} resource={}", variant.name(), variant.getClasspathResource(), e);
            return null;
        }
    }

    private static final String NUDENET_CLASSPATH_RESOURCE = "nudenet_640m.onnx";

    /**
     * 解析 NudeNet 640m ONNX：外部 {@code nudenet.model-path} 优先，否则 classpath {@code nudenet_640m.onnx}。
     */
    public ResolvedFileModel resolveNudeNet(GroupModerationConfig config) {
        NudeNetConfig nudeNet = config.getNudenet();
        if (nudeNet == null || !nudeNet.isEnable()) {
            return null;
        }
        int inputSize = nudeNet.getInputSize() > 0 ? nudeNet.getInputSize() : 640;

        if (StringUtils.hasText(nudeNet.getModelPath())) {
            File external = new File(nudeNet.getModelPath());
            if (external.isFile()) {
                return new ResolvedFileModel(external, inputSize, false);
            }
            log.warn("配置的 nudenet.model-path 不存在，将尝试内置模型: {}", external.getAbsolutePath());
        }

        try {
            File classpathModel = materializeResource(NUDENET_CLASSPATH_RESOURCE, "qbot-nudenet", ".onnx");
            return new ResolvedFileModel(classpathModel, inputSize, true);
        } catch (Exception e) {
            log.error("加载内置 NudeNet ONNX 失败 resource={}", NUDENET_CLASSPATH_RESOURCE, e);
            return null;
        }
    }

    private File materializeClasspathModel(OnnxModelVariant variant) throws Exception {
        String resourcePath = variant.getClasspathResource();
        String suffix = resourcePath.contains("299") ? "_299.onnx" : "_224.onnx";
        return materializeResource(resourcePath, "qbot-nsfw", suffix);
    }

    private File materializeResource(String resourcePath, String tempPrefix, String tempSuffix) throws Exception {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = OnnxModelLoader.class.getClassLoader();
        }
        try (InputStream in = cl.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("classpath 中未找到模型: " + resourcePath);
            }
            File temp = File.createTempFile(tempPrefix, tempSuffix);
            temp.deleteOnExit();
            Files.copy(in, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log.info("已从 classpath 释放内置模型: {} -> {}", resourcePath, temp.getAbsolutePath());
            return temp;
        }
    }

    /**
     * {@link #resolve} 成功后的模型路径与输入尺寸。
     */
    @Value
    public static class ResolvedModel {
        File modelFile;
        int inputSize;
        OnnxModelVariant variant;
        boolean fromClasspath;
    }

    @Value
    public static class ResolvedFileModel {
        File modelFile;
        int inputSize;
        boolean fromClasspath;
    }
}
