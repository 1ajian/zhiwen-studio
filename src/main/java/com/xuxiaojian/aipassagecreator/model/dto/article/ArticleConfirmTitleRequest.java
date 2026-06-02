package com.xuxiaojian.aipassagecreator.model.dto.article;

import lombok.Data;

import java.io.Serializable;

/**
 * ClassName: ArticleConfirmTitleRequest
 * Package: com.xuxiaojian.aipassagecreator.model.dto.article
 * Description:
 *
 * @Author 阿健
 * @Create 2026-06-01 21:40
 * @Version 1.0
 */
@Data
public class ArticleConfirmTitleRequest implements Serializable {

    private static final long serialVersionUID = -5337601220666036762L;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 选中的主标题
     */
    private String selectedMainTitle;

    /**
     * 选中的副标题
     */
    private String selectedSubTitle;

    /**
     * 用户补充描述（可选）
     */
    private String userDescription;
}
