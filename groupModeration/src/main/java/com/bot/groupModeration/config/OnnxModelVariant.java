package com.bot.groupModeration.config;

import lombok.Getter;

/**
 * 内置 ONNX 模型规格：224（MobileNet）与 299（Inception）。
 */
@Getter
public enum OnnxModelVariant {

    /**
     * classpath: nsfw_mobilenet2_224x224.onnx
     */
    V224(224, "nsfw_mobilenet2_224x224.onnx"),

    /**
     * classpath: nsfw_inception_v3_299x299.onnx
     */
    V299(299, "nsfw_inception_v3_299x299.onnx");

    private final int inputSize;
    private final String classpathResource;

    OnnxModelVariant(int inputSize, String classpathResource) {
        this.inputSize = inputSize;
        this.classpathResource = classpathResource;
    }

    /**
     * 解析配置值，支持 224 / 299 / mobilenet224 / inception299（不区分大小写）。
     */
    public static OnnxModelVariant from(String value) {
        if (value == null || value.trim().isEmpty()) {
            return V224;
        }
        String v = value.trim().toLowerCase();
        switch (v) {
            case "299":
            case "v299":
            case "inception":
            case "inception299":
                return V299;
            case "224":
            case "v224":
            case "mobilenet":
            case "mobilenet224":
            default:
                return V224;
        }
    }
}
