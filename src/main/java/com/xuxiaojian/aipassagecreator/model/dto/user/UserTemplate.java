package com.xuxiaojian.aipassagecreator.model.dto.user;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * ClassName: UserTemplate
 * Package: com.xuxiaojian.aipassagecreator.model.dto.user
 * Description:
 *
 * @Author 阿健
 * @Create 2026-06-04 16:00
 * @Version 1.0
 */
@Data
public class UserTemplate {

    @ExcelProperty("账号")
    private String userAccount;

    @ExcelProperty("密码")
    private String userPassword;

    @ExcelProperty("用户名")
    private String userName;

    @ExcelProperty("用户简介")
    private String userProfile;

    @ExcelProperty("用户角色")
    private String userRole;
}
