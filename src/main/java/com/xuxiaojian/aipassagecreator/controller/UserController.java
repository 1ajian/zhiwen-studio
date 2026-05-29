package com.xuxiaojian.aipassagecreator.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.xuxiaojian.aipassagecreator.annotation.AuthCheck;
import com.xuxiaojian.aipassagecreator.common.BaseResponse;
import com.xuxiaojian.aipassagecreator.common.DeleteRequest;
import com.xuxiaojian.aipassagecreator.common.ResultUtils;
import com.xuxiaojian.aipassagecreator.constant.UserConstant;
import com.xuxiaojian.aipassagecreator.exception.BusinessException;
import com.xuxiaojian.aipassagecreator.exception.ErrorCode;
import com.xuxiaojian.aipassagecreator.exception.ThrowUtils;
import com.xuxiaojian.aipassagecreator.model.dto.user.*;
import com.xuxiaojian.aipassagecreator.model.entity.User;
import com.xuxiaojian.aipassagecreator.model.vo.LoginUserVO;
import com.xuxiaojian.aipassagecreator.model.vo.UserVO;
import com.xuxiaojian.aipassagecreator.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ClassName: UserController
 * Package: com.xuxiaojian.aipassagecreator.controller
 * Description:
 *
 * @Author 阿健
 * @Create 2026-05-26 21:33
 * @Version 1.0
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 注册接口
     *
     * @param userRegisterRequest
     * @return
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        Long result = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(result);

    }

    /**
     * 用户登录
     *
     * @param userLoginRequest
     * @param request
     * @return
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 获取登录用户的脱敏信息
     *
     * @param request
     * @return
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }

    /**
     * 用户退出登录/注销
     *
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        Boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取包装类
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(Long id) {
        BaseResponse<User> response = getUserById(id);
        User user = response.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 更新用户信息（包含修改密码）
     * @param userUpdateBySelfDto
     * @param request
     * @return
     */
    @PostMapping("/updateBySelf")
    public BaseResponse<Boolean> updateByUser(@RequestBody UserUpdateBySelfDto userUpdateBySelfDto, HttpServletRequest request) {
        ThrowUtils.throwIf(userUpdateBySelfDto == null,ErrorCode.PARAMS_ERROR,"请求参数为空");
        String newUserPassword = userUpdateBySelfDto.getNewUserPassword();
        String oldUserPassword = userUpdateBySelfDto.getOldUserPassword();
        User user = new User();
        BeanUtil.copyProperties(userUpdateBySelfDto,user);
        if (StrUtil.isNotBlank(newUserPassword) && StrUtil.isNotBlank(oldUserPassword)) {
            User loginUser = userService.getLoginUser(request);
            String userPassword = loginUser.getUserPassword();
            if (!userPassword.equals(userService.getEncryptPassword(oldUserPassword))) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"当前密码错误");
            }
            String newEncryptPassword = userService.getEncryptPassword(newUserPassword);
            user.setUserPassword(newEncryptPassword);
        }
        boolean updateResult = userService.updateById(user);
        //如果是修改密码如果删除登录态
        if (StrUtil.isNotBlank(newUserPassword) && StrUtil.isNotBlank(oldUserPassword)) {
            request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        }
        return ResultUtils.success(updateResult);

    }

    /**
     * 高管创建用户
     *
     * @param userAddRequest
     * @return
     */
    @PostMapping("/add")
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtil.copyProperties(userAddRequest, user);
        // 默认密码 12345678
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * 高管删除用户信息
     *
     * @param deleteRequest
     * @return
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest == null, ErrorCode.PARAMS_ERROR);
        Long userId = deleteRequest.getId();
        boolean removeResult = userService.removeById(userId);
        ThrowUtils.throwIf(!removeResult, ErrorCode.OPERATION_ERROR, "用户删除失败");
        return ResultUtils.success(removeResult);
    }

    /**
     * 高管根据 id 获取用户详情信息
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(Long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }


    /**
     * 高管更新用户
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userPassword = userUpdateRequest.getUserPassword();
        if (StrUtil.isNotBlank(userPassword)) {
            String encryptPassword = userService.getEncryptPassword(userPassword);
            userUpdateRequest.setUserPassword(encryptPassword);
        }
        User user = new User();
        BeanUtil.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(result);
    }

    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Long pageNum = userQueryRequest.getPageNum();
        Long pageSize = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(new Page<>(pageNum, pageSize),
                userService.getQueryWrapper(userQueryRequest));

        Page<UserVO> userVOPage = new Page<>(pageNum, pageSize, userPage.getTotalRow());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);

    }
}
