package com.xuxiaojian.aipassagecreator.model.dto.image;

import lombok.Builder;
import lombok.Data;

/**
 * ClassName: ImageRequest
 * Package: com.xuxiaojian.aipassagecreator.model.dto.image
 * Description:
 *  图片请求对象
 *  统一封装图片获取所需的各种参数,便于扩展
 * @Author 阿健
 * @Create 2026-05-29 13:31
 * @Version 1.0
 */
@Data
@Builder
public class ImageRequest {

    /**
     * 搜索关键字（用于图库检索）
     */
    private String keywords;

    /**
     * 生图提示词（用于AI生图）
     */
    private String prompt;

    /**
     * 图片位置序号
     */
    private Integer position;

    /**
     * 图片类型（cover/section）
     */
    private String type;

    /**
     * 宽高比（如16:9，1:1）
     */
    private String aspectRatio;

    /**
     * 图片风格描述
     */
    private String style;


    /**
     * 获取有效的搜索/生成参数
     * AI生图优先使用prompt，图库检索使用keywords
     * @param isAiGenerated
     * @return
     */
    public String getEffectiveParam(boolean isAiGenerated) {
        if (isAiGenerated) {
            return prompt != null && !prompt.isEmpty() ? prompt : keywords;
        }

        return keywords != null && !keywords.isEmpty() ? keywords : prompt;
    }
}
