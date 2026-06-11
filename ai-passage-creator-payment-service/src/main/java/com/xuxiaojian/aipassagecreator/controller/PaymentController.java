package com.xuxiaojian.aipassagecreator.controller;

import com.xuxiaojian.aipassagecreator.annotation.AuthCheck;
import com.xuxiaojian.aipassagecreator.common.BaseResponse;
import com.xuxiaojian.aipassagecreator.common.ResultUtils;
import com.xuxiaojian.aipassagecreator.constant.UserConstant;
import com.xuxiaojian.aipassagecreator.exception.BusinessException;
import com.xuxiaojian.aipassagecreator.exception.ErrorCode;
import com.xuxiaojian.aipassagecreator.model.entity.PaymentRecord;
import com.xuxiaojian.aipassagecreator.security.SessionUser;
import com.xuxiaojian.aipassagecreator.service.PaymentService;
import com.xuxiaojian.aipassagecreator.security.SessionUserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ClassName: PaymentController
 * Package: com.xuxiaojian.aipassagecreator.controller
 * Description:
 *
 * @Author 阿健
 * @Create 2026-06-02 21:43
 * @Version 1.0
 */
@RestController
@RequestMapping("/payment")
@Slf4j
@Tag(name = "支付接口")
public class PaymentController {

    @Resource
    private PaymentService paymentService;

    @PostMapping("/create-vip-session")
    @Operation(summary = "创建 VIP 支付会话")
    public BaseResponse<String> createVipPaymentSession(HttpServletRequest request) {
        SessionUser loginUser = SessionUserHolder.getRequiredLoginUser();

        try {
            String sessionUrl = paymentService.createVipPaymentSession(loginUser.getId());
            return ResultUtils.success(sessionUrl);
        }catch (Exception e ) {
            log.error("创建支付会话失败，" + e.getMessage(),e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"创建支付会话失败");
        }
    }

    @PostMapping("/refund")
    @Operation(summary = "申请退款")
    @AuthCheck(mustRole = UserConstant.VIP_ROLE)
    public BaseResponse<Boolean> refund(@RequestParam(required = false) String reason,HttpServletRequest request) {
        SessionUser loginUser = SessionUserHolder.getRequiredLoginUser();
        try {
            boolean success = paymentService.handleRefund(loginUser.getId(), reason);
            return ResultUtils.success(success);
        }catch (Exception e) {
            log.error("退款失败",e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"退款失败");
        }
    }

    @GetMapping("/records")
    @Operation(summary = "获取当前用户支付记录")
    public BaseResponse<List<PaymentRecord>> getPaymentRecords(HttpServletRequest request) {
        SessionUser loginUser = SessionUserHolder.getRequiredLoginUser();
        List<PaymentRecord> paymentRecords = paymentService.getPaymentRecords(loginUser.getId());
        return ResultUtils.success(paymentRecords);
    }
}
