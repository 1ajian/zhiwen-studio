package com.xuxiaojian.aipassagecreator.controller;

import com.mybatisflex.core.query.QueryWrapper;
import com.xuxiaojian.aipassagecreator.api.statistics.dto.UserStatisticsDTO;
import com.xuxiaojian.aipassagecreator.api.user.dto.QuotaConsumeRequest;
import com.xuxiaojian.aipassagecreator.api.user.dto.QuotaIncreaseRequest;
import com.xuxiaojian.aipassagecreator.api.user.dto.UserPaymentProfileDTO;
import com.xuxiaojian.aipassagecreator.api.user.dto.VipUpdateRequest;
import com.xuxiaojian.aipassagecreator.common.BaseResponse;
import com.xuxiaojian.aipassagecreator.common.ResultUtils;
import com.xuxiaojian.aipassagecreator.constant.UserConstant;
import com.xuxiaojian.aipassagecreator.exception.ErrorCode;
import com.xuxiaojian.aipassagecreator.exception.ThrowUtils;
import com.xuxiaojian.aipassagecreator.mapper.UserMapper;
import com.xuxiaojian.aipassagecreator.model.entity.User;
import com.xuxiaojian.aipassagecreator.service.QuotaService;
import com.xuxiaojian.aipassagecreator.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户服务内部接口。
 * 跨服务的用户库读写统一收口到这里，避免文章、支付、统计服务直连用户表。
 */
@RestController
@RequestMapping("/internal/user")
public class InternalUserController {

    @Resource
    private UserService userService;

    @Resource
    private QuotaService quotaService;

    @Resource
    private UserMapper userMapper;

    /**
     * 额度检查和扣减接口
     * @param request
     * @return
     */
    @PostMapping("/quota/check-consume")
    public BaseResponse<Boolean> checkAndConsumeQuota(@RequestBody QuotaConsumeRequest request) {
        ThrowUtils.throwIf(request == null || request.getUserId() == null, ErrorCode.PARAMS_ERROR);
        User user = requireUser(request.getUserId());
        quotaService.checkAndConsumeQuota(user);
        return ResultUtils.success(true);
    }

    /**
     * 配额补偿请求接口
     * @param request
     * @return
     */
    @PostMapping("/quota/increase")
    public BaseResponse<Boolean> increaseQuota(@RequestBody QuotaIncreaseRequest request) {
        ThrowUtils.throwIf(request == null || request.getUserId() == null, ErrorCode.PARAMS_ERROR);
        int amount = request.getAmount() == null || request.getAmount() <= 0 ? 1 : request.getAmount();
        return ResultUtils.success(userMapper.increaseQuota(request.getUserId(), amount) > 0);
    }

    /**
     * 激活VIP接口
     * 支付服务发起 Seata 全局事务时，这里作为用户库本地事务分支参与提交或回滚。
     *
     * @param request
     * @return
     */
    @PostMapping("/vip/activate")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<Boolean> activateVip(@RequestBody VipUpdateRequest request) {
        ThrowUtils.throwIf(request == null || request.getUserId() == null, ErrorCode.PARAMS_ERROR);
        User user = requireUser(request.getUserId());
        user.setVipTime(LocalDateTime.now());
        if (!UserConstant.ADMIN_ROLE.equals(user.getUserRole())) {
            user.setUserRole(UserConstant.VIP_ROLE);
        }
        return ResultUtils.success(userService.updateById(user));
    }

    /**
     * 取消VIP接口
     * 支付退款补偿走 Seata 全局事务时，这里作为用户库本地事务分支参与回滚控制。
     *
     * @param request
     * @return
     */
    @PostMapping("/vip/cancel")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<Boolean> cancelVip(@RequestBody VipUpdateRequest request) {
        ThrowUtils.throwIf(request == null || request.getUserId() == null, ErrorCode.PARAMS_ERROR);
        User user = requireUser(request.getUserId());
        user.setVipTime(null);
        if (!UserConstant.ADMIN_ROLE.equals(user.getUserRole())) {
            user.setUserRole(UserConstant.DEFAULT_ROLE);
        }
        //throw new BusinessException(ErrorCode.SYSTEM_ERROR,"恶意错误");
        return ResultUtils.success(userService.updateById(user,false));
    }

    /**
     * 用户统计相关接口
     * @return
     */
    @GetMapping("/statistics")
    public BaseResponse<UserStatisticsDTO> getUserStatistics() {
        Long totalUserCount = userMapper.selectCountByQuery(QueryWrapper.create());
        Long vipUserCount = userMapper.selectCountByQuery(QueryWrapper.create().eq("userRole", UserConstant.VIP_ROLE));
        List<User> users = userMapper.selectListByQuery(QueryWrapper.create().eq("userRole", UserConstant.DEFAULT_ROLE));
        long remainQuota = users.stream().mapToInt(user -> user.getQuota() == null ? 0 : user.getQuota()).sum();
        long quotaUsed = users.size() * UserConstant.DEFAULT_QUOTA - remainQuota;
        UserStatisticsDTO statistics = UserStatisticsDTO.builder()
                .totalUserCount(totalUserCount)
                .vipUserCount(vipUserCount)
                .quotaUsed(quotaUsed)
                .build();
        return ResultUtils.success(statistics);
    }

    /**
     * 用户资料接口（支付使用）
     * @param userId
     * @return
     */
    @GetMapping("/profile/{userId}")
    public BaseResponse<UserPaymentProfileDTO> getUserPaymentProfile(@PathVariable Long userId) {
        User user = requireUser(userId);
        UserPaymentProfileDTO profile = new UserPaymentProfileDTO();
        profile.setId(user.getId());
        profile.setUserRole(user.getUserRole());
        profile.setVipTime(user.getVipTime());
        profile.setQuota(user.getQuota());
        return ResultUtils.success(profile);
    }

    private User requireUser(Long userId) {
        User user = userService.getById(userId);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        return user;
    }
}
