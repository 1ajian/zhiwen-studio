package com.xuxiaojian.aipassagecreator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import static com.xuxiaojian.aipassagecreator.constant.ArticleConstant.SVG_DEFAULT_HEIGHT;
import static com.xuxiaojian.aipassagecreator.constant.ArticleConstant.SVG_DEFAULT_WIDTH;

/**
 * ClassName: SvgDiagramConfig
 * Package: com.xuxiaojian.aipassagecreator.config
 * Description:
 *  SVG 概念示意图生成配置
 * @Author 阿健
 * @Create 2026-05-30 2:36
 * @Version 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "svg-diagram")
@Data
public class SvgDiagramConfig {

    /**
     * 默认宽度
     */
    private Integer defaultWidth = SVG_DEFAULT_WIDTH;

    /**
     * 默认高度
     */
    private Integer defaultHeight = SVG_DEFAULT_HEIGHT;

    /**
     * COS 存储文件夹
     */
    private String folder = "svg-diagrams";
}
