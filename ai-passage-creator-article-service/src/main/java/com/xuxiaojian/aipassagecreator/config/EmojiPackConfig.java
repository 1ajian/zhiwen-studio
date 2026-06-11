package com.xuxiaojian.aipassagecreator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import static com.xuxiaojian.aipassagecreator.constant.ArticleConstant.BING_IMAGE_SEARCH_URL;
import static com.xuxiaojian.aipassagecreator.constant.ArticleConstant.EMOJI_PACK_SUFFIX;

/**
 * ClassName: EmojiPackConfig
 * Package: com.xuxiaojian.aipassagecreator.config
 * Description:
 *
 * @Author 阿健
 * @Create 2026-05-30 2:23
 * @Version 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "emoji-pack")
@Data
public class EmojiPackConfig {

    /**
     * Bing 图片搜索地址
     */
    private String searchUrl = BING_IMAGE_SEARCH_URL;

    /**
     * 表情包关键词后缀（程序固定拼接，不依赖 AI 返回）
     */
    private String suffix = EMOJI_PACK_SUFFIX;

    /**
     * 请求超时时间（毫秒）
     */
    private Integer timeout = 10000;
}
