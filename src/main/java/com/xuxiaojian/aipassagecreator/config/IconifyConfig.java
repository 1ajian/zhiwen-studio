package com.xuxiaojian.aipassagecreator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * ClassName: IconifyConfig
 * Package: com.xuxiaojian.aipassagecreator.config
 * Description:
 *
 * @Author 阿健
 * @Create 2026-05-30 1:44
 * @Version 1.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "iconify")
public class IconifyConfig {

    /**
     * API 地址
     */
    private String apiUrl = "https://api.iconify.design";

    /**
     * 搜索结果限制数量
     */
    private Integer searchLimit = 10;

    /**
     * 默认图标高度
     */
    private Integer defaultHeight = 64;

    /**
     * 默认图标颜色（留空使用 currentColor,或设置如"#000000"）
     */
    private String defaultColor = "";
}
