package com.xuxiaojian.aipassagecreator.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.xuxiaojian.aipassagecreator.exception.BusinessException;
import com.xuxiaojian.aipassagecreator.exception.ErrorCode;
import com.xuxiaojian.aipassagecreator.exception.ThrowUtils;
import com.xuxiaojian.aipassagecreator.mapper.UserMapper;
import com.xuxiaojian.aipassagecreator.model.dto.user.UserQueryRequest;
import com.xuxiaojian.aipassagecreator.model.dto.user.UserTemplate;
import com.xuxiaojian.aipassagecreator.model.entity.User;
import com.xuxiaojian.aipassagecreator.model.enums.UserRoleEnum;
import com.xuxiaojian.aipassagecreator.model.vo.LoginUserVO;
import com.xuxiaojian.aipassagecreator.model.vo.UserVO;
import com.xuxiaojian.aipassagecreator.service.UserService;
import com.xuxiaojian.aipassagecreator.utils.excel.UserImportListener;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.xuxiaojian.aipassagecreator.constant.UserConstant.USER_LOGIN_STATE;

/**
 * ClassName: UserServiceImpl
 * Package: com.xuxiaojian.aipassagecreator.service.impl
 * Description:
 *
 * @Author 阿健
 * @Create 2026-05-26 20:31
 * @Version 1.0
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {


    @Value(value = "${user.password.salt}")
    private String SALT;

    @Resource
    @Lazy
    private UserImportListener userImportListener;

    @Resource
    private UserMapper userMapper;

    @Override
    public Long userRegister(String userAccount, String userPassword, String checkPassword) {
        boolean paramHasBlank = StrUtil.hasBlank(userAccount, userPassword, checkPassword);
        ThrowUtils.throwIf(paramHasBlank, ErrorCode.PARAMS_ERROR, "参数为空");
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度过短");
        }

        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度过短");
        }

        ThrowUtils.throwIf(!userPassword.equals(checkPassword), ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");

        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.mapper.selectCountByQuery(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已存在,请直接登录");
        }

        String encryptPassword = getEncryptPassword(userPassword);
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("大侠-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6));
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean userSaveResult = this.save(user);
        if (!userSaveResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "注册失败,请联系客服解决");
        }

        return user.getId();
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        //1.校验参数
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }

        String encryptPassword = getEncryptPassword(userPassword);

        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.mapper.selectOneByQuery(queryWrapper);
        ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR, "用户不存在或者密码错误");
        request.getSession().setAttribute(USER_LOGIN_STATE, user);

        return this.getLoginUserVO(user);
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        Long userId = currentUser.getId();
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "当前登录用户不存在,请重新登录");
        }

        return user;
    }

    @Override
    public Boolean userLogout(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户未登录");
        }

        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    /**
     * 获取脱敏用户信息
     *
     * @param user
     * @return
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    /**
     * 获取加密密码
     *
     * @param userPassword
     * @return
     */
    @Override
    public String getEncryptPassword(String userPassword) {
        return DigestUtils.md5DigestAsHex((userPassword + SALT).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * user对象转userVO对象
     *
     * @param user
     * @return
     */
    @Override
    public UserVO getUserVO(User user) {
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * 获取QueryWrapper对象
     *
     * @param userQueryRequest
     * @return
     */
    @Override
    public QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数为空");
        }

        Long id = userQueryRequest.getId();
        String userName = userQueryRequest.getUserName();
        String userAccount = userQueryRequest.getUserAccount();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();

        return QueryWrapper.create()
                .eq("id",id)
                .eq("userRole",userRole)
                .like("userAccount",userAccount)
                .like("userName",userName)
                .like("userProfile",userProfile)
                .orderBy(sortField,"ascend".equals(sortOrder));
    }

    /**
     * user列表转userVO列表
     * @param userList
     * @return
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        List<UserVO> userVOList = userList.stream()
                .map(this::getUserVO).collect(Collectors.toList());
        return userVOList;
    }



    @Override
    public Boolean importAll(MultipartFile multipartFile) {
        try {
            EasyExcel.read(multipartFile.getInputStream(), UserTemplate.class,userImportListener)
                    .sheet()
                    .headRowNumber(2)
                    .doRead();
        } catch (IOException e) {
            log.info("导入数据异常," + e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"导入数据异常," + e.getMessage());
        }

        return true;
    }

    @Override
    public void exportAll(HttpServletResponse response)  {
        long startTime = System.currentTimeMillis();

        ClassPathResource classPathResource = new ClassPathResource("templates/user-import-template.xlsx");
        if (!classPathResource.exists()) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"本地模板不存在");
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        try (InputStream templateInputStream = classPathResource.getInputStream();
             ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream(), UserTemplate.class)
                     .needHead(Boolean.FALSE)
                     .relativeHeadRowIndex(2)
                     .withTemplate(templateInputStream)
                     .build()) {
            String encodedFileName = URLEncoder.encode("系统用户数据信息", "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename=" + encodedFileName + ".xlsx");

            // 创建具体的 Sheet 页
            WriteSheet writeSheet = EasyExcel
                    .writerSheet(0)
                    .build();
            int pageNum = 1;
            int size = 5000;

            while (true) {
                log.info("开始导出第 {} 页数据...", pageNum);
                Page<User> page = new Page<>(pageNum,size);
                Page<User> userPage = userMapper.paginate(page, QueryWrapper.create());
                List<User> records = userPage.getRecords();
                List<UserTemplate> userTemplateList = records.stream().map(record -> {
                    UserTemplate userTemplate = new UserTemplate();
                    BeanUtil.copyProperties(record, userTemplate);
                    return userTemplate;
                }).collect(Collectors.toList());
                if (CollUtil.isEmpty(userTemplateList)) {
                    log.info("数据读取完毕，共导出页数: {}", pageNum - 1);
                    break;
                }

                excelWriter.write(userTemplateList,writeSheet);

                pageNum++;
            }

            long endTime = System.currentTimeMillis();
            log.info("Excel 导出成功！总耗时: {} ms", (endTime - startTime));
            response.flushBuffer();

        } catch (Exception e) {
            log.info("导出失败,{}",e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,e.getMessage());
        }
    }
}
