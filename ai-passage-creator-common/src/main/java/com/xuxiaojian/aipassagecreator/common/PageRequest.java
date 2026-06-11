package com.xuxiaojian.aipassagecreator.common;

import lombok.Data;

/**
 * ClassName: PageRequest
 * Package: com.xuxiaojian.aipassagecreator.common
 * Description:
 *  分页请求类
 * @Author 阿健
 * @Create 2026-05-25 23:31
 * @Version 1.0
 */
@Data
public class PageRequest {

    private Long pageNum = 1L;

    private Long pageSize = 10L;

    private String sortField;

    private String sortOrder = "descend";
}
