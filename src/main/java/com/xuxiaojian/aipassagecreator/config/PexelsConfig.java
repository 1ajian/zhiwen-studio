package com.xuxiaojian.aipassagecreator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * ClassName: PexelsConfig
 * Package: com.xuxiaojian.aipassagecreator.config
 * Description:
 *
 * @Author 阿健
 * @Create 2026-05-27 21:56
 * @Version 1.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "pexels")
public class PexelsConfig {

    /**
     * API Key
     */
    private String apiKey;
}
