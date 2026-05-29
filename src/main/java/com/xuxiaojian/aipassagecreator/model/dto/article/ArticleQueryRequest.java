package com.xuxiaojian.aipassagecreator.model.dto.article;

import com.xuxiaojian.aipassagecreator.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * ClassName: ArticleQueryRequest
 * Package: com.xuxiaojian.aipassagecreator.model.dto.article
 * Description:
 *
 * @Author 阿健
 * @Create 2026-05-28 23:13
 * @Version 1.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ArticleQueryRequest extends PageRequest implements Serializable {


    private static final long serialVersionUID = -2036825120092405175L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 状态
     */
    private String status;

}
