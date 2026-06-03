package com.xuxiaojian.aipassagecreator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * ClassName: NanoBananaConfig
 * Package: com.xuxiaojian.aipassagecreator.config
 * Description:
 *
 * @Author 阿健
 * @Create 2026-05-29 16:24
 * @Version 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "nano-banana")
@Data
public class NanoBananaConfig {

    private String apiKey;

    /**
     * 是否为 Nano Banana 单独启用代理。
     * 启用后仅当前 Google GenAI 客户端走代理，不影响进程内其他 HTTP 请求。
     */
    private Boolean proxyEnabled = true;

    /**
     * Nano Banana 专用代理主机。
     */
    private String proxyHost = "127.0.0.1";

    /**
     * Nano Banana 专用代理端口。
     */
    private Integer proxyPort = 7892;

    /**
     * 模型名称
     * gemini-2.5-flash-image: 速度快，适合高吞吐低延迟
     * gemini-3-pro-image-preview: 专业级，支持高级推理和高分辨率
     */
    private String model = "gemini-2.5-flash-image";

    /**
     * 图片宽高比
     * 支持: 1:1, 2:3, 3:2, 3:4, 4:3, 4:5, 5:4, 9:16, 16:9, 21:9
     */
    private String aspectRatio = "16:9";

    /**
     * 图片分辨率（仅 gemini-3-pro-image-preview 支持）
     * 支持: 1K, 2K, 4K
     */
    private String imageSize = "1K";

    /**
     * 输出图片格式
     * 支持: image/jpeg, image/png
     */
    private String outputMimeType = "image/png";

}
