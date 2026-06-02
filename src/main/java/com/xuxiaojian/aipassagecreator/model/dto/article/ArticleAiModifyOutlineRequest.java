package com.xuxiaojian.aipassagecreator.model.dto.article;

import lombok.Data;

import java.io.Serializable;

/**
 * ClassName: ArticleAiModifyOutlineRequest
 * Package: com.xuxiaojian.aipassagecreator.model.dto.article
 * Description:
 *
 * @Author 阿健
 * @Create 2026-06-01 21:51
 * @Version 1.0
 */
@Data
public class ArticleAiModifyOutlineRequest implements Serializable {

    private static final long serialVersionUID = 1320005164705113443L;

    /**
     * 任务Id
     */
    private String taskId;

    /**
     * 用户的修改建议
     */
    private String modifySuggestion;

}
