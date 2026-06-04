package com.xuxiaojian.aipassagecreator.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.xuxiaojian.aipassagecreator.model.dto.user.UserQueryRequest;
import com.xuxiaojian.aipassagecreator.model.entity.User;
import com.xuxiaojian.aipassagecreator.model.vo.LoginUserVO;
import com.xuxiaojian.aipassagecreator.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * ClassName: UserService
 * Package: com.xuxiaojian.aipassagecreator.service
 * Description:
 *
 * @Author 阿健
 * @Create 2026-05-26 20:30
 * @Version 1.0
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount
     * @param userPassword
     * @param checkPassword
     * @return
     */
    Long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     *
     * @param userAccount
     * @param userPassword
     * @param request
     * @return
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取当前登录用户信息
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户注销,退出登录
     *
     * @param request
     * @return
     */
    Boolean userLogout(HttpServletRequest request);

    /**
     * 获取脱敏用户信息
     *
     * @param user
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取加密密码信息
     *
     * @param userPassword
     * @return
     */
    String getEncryptPassword(String userPassword);

    /**
     * User对象转UserVO
     *
     * @param user
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 根据参数获取QueryWrapper对象
     *
     * @param userQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * user列表转userVO列表
     * @param records
     * @return
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 批量导入
     * @param multipartFile
     * @return
     */
    Boolean importAll(MultipartFile multipartFile);

    /**
     * 批量导出
     * @param response
     */
    void exportAll(HttpServletResponse response);
}
