package com.xuxiaojian.aipassagecreator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * ClassName: CosConfig
 * Package: com.xuxiaojian.aipassagecreator.config
 * Description:
 *
 * @Author 阿健
 * @Create 2026-05-27 19:55
 * @Version 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "tencent.cos")
@Data
public class CosConfig {

    /**
     * 域名
     */
    private String host;

    /**
     * Secret ID
     */
    private String secretId;

    /**
     * Secret Key
     */
    private String secretKey;

    /**
     * 地域
     */
    private String region;

    /**
     * 存储桶
     */
    private String bucket;
}
