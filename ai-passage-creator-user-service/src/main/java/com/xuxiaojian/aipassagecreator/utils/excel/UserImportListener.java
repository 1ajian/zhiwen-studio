package com.xuxiaojian.aipassagecreator.utils.excel;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.xuxiaojian.aipassagecreator.constant.UserConstant;
import com.xuxiaojian.aipassagecreator.mapper.UserMapper;
import com.xuxiaojian.aipassagecreator.model.dto.user.UserTemplate;
import com.xuxiaojian.aipassagecreator.model.entity.User;
import com.xuxiaojian.aipassagecreator.service.UserService;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ClassName: UserImportListener
 * Package: com.xuxiaojian.aipassagecreator.utils
 * Description:
 *
 * @Author 阿健
 * @Create 2026-06-04 16:11
 * @Version 1.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Component
@Slf4j
public class UserImportListener extends AnalysisEventListener<UserTemplate> {

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserService userService;

    private static final int BATCH_COUNT = 100;

    private int currentCount = 0;

    private List<UserTemplate> userList = new ArrayList<>();

    @Override
    public void invoke(UserTemplate userTemplate, AnalysisContext analysisContext) {
        // 行号
        Integer index = analysisContext.readRowHolder().getRowIndex();
        log.info("解析到第{}行的数据:{}",index,userTemplate);
        currentCount++;
        userList.add(userTemplate);

        if (userList.size() >= BATCH_COUNT) {
            List<User> users = userList.stream().map(u -> {
                u.setUserPassword(userService.getEncryptPassword(u.getUserPassword()));
                User user = new User();
                BeanUtil.copyProperties(u, user);
                user.setEditTime(LocalDateTime.now());
                user.setUpdateTime(LocalDateTime.now());
                user.setCreateTime(LocalDateTime.now());
                user.setQuota((int)UserConstant.DEFAULT_QUOTA.longValue());
                user.setIsDelete(0);
                return user;
            }).collect(Collectors.toList());
            userMapper.insertBatch(users);
            userList.clear();
        }
    }


    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        //可能存在一部分数据暂时未处理
        if (!userList.isEmpty()){
            List<User> users = BeanUtil.copyToList(userList, User.class);
            users = users.stream().map(user ->  {
                user.setUserPassword(userService.getEncryptPassword(user.getUserPassword()));
                user.setEditTime(LocalDateTime.now());
                user.setUpdateTime(LocalDateTime.now());
                user.setCreateTime(LocalDateTime.now());
                user.setQuota((int)UserConstant.DEFAULT_QUOTA.longValue());
                user.setIsDelete(0);
                return user;
            }).collect(Collectors.toList());
            userMapper.insertBatch(users);
            userList.clear();
        }
    }
}
