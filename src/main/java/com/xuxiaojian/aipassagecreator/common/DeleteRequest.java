package com.xuxiaojian.aipassagecreator.common;

import lombok.Data;

import java.io.Serializable;

/**
 * ClassName: DeleteRequest
 * Package: com.xuxiaojian.aipassagecreator.common
 * Description:
 *  删除请求类
 * @Author 阿健
 * @Create 2026-05-25 23:30
 * @Version 1.0
 */
@Data
public class DeleteRequest implements Serializable {

    private static final long serialVersionUID = 3465157529864015856L;

    private Long id;
}
