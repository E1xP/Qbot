# groupModeration

群图片 ONNX 审核：消息入队 → 单线程推理 → 命中后按 `messageId` 处置。

配置见 `application-groupModeration.yml`。内置模型：`nsfw_mobilenet2_224x224.onnx`（224）、`nsfw_inception_v3_299x299.onnx`
（299）。

插件须注册在 `FilterPlugin` 之前。
