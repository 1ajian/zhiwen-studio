package com.xuxiaojian.aipassagecreator.model.dto.article;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * ClassName: ArticleCreateRequest
 * Package: com.xuxiaojian.aipassagecreator.model.dto.article
 * Description:
 *
 * @Author 阿健
 * @Create 2026-05-29 0:10
 * @Version 1.0
 */
@Data
public class ArticleCreateRequest implements Serializable {

    private static final long serialVersionUID = 3686297560374504808L;

    /**
     * 选题
     */
    private String topic;

    /**
     * 文章风格：tech/emotional/educational/humorous，可为空
     */
    private String style;

    /**
     * 允许的配图方式列表（为空或 null 表示支持所有方式）
     * 可选值：PEXELS, NANO_BANANA, MERMAID, ICONIFY, EMOJI_PACK, SVG_DIAGRAM
     */
    private List<String> enabledImageMethods;

}
