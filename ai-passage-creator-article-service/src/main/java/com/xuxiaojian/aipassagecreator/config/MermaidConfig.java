package com.xuxiaojian.aipassagecreator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * ClassName: MermaidConfig
 * Package: com.xuxiaojian.aipassagecreator.config
 * Description:
 *
 * @Author 阿健
 * @Create 2026-05-30 0:11
 * @Version 1.0
 */

@Configuration
@ConfigurationProperties(prefix = "mermaid")
@Data
public class MermaidConfig {

    /**
     * CLI命令 （Windows下为 mmdc.cmd ,Linux/Max 下为mmdc）
     */
    private String cliCommand = "mmdc";

    /**
     * 背景颜色（transparent 为透明背景）
     */
    private String backgroundColor = "transparent";

    /**
     * 输出格式（svg/png/pdf）
     */
    private String outputFormat = "svg";

    /**
     * 图片宽度（像素）
     */
    private Integer width = 1200;

    /**
     * 命令执行超时时间（毫秒）
     */
    private Long timeout = 30000L;
}
