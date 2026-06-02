package com.xuxiaojian.aipassagecreator.model.dto.article;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * ClassName: ArticleConfirmOutlineRequest
 * Package: com.xuxiaojian.aipassagecreator.model.dto.article
 * Description:
 *
 * @Author 阿健
 * @Create 2026-06-01 21:48
 * @Version 1.0
 */
@Data
public class ArticleConfirmOutlineRequest implements Serializable {

    private static final long serialVersionUID = 6739874505454068131L;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 用户编辑后的大纲
     */
    private List<ArticleState.OutlineSection> outline;
}
