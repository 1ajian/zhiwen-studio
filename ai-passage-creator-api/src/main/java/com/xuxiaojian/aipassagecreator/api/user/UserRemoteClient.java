package com.xuxiaojian.aipassagecreator.api.user;

import com.xuxiaojian.aipassagecreator.api.statistics.dto.UserStatisticsDTO;
import com.xuxiaojian.aipassagecreator.api.user.dto.QuotaConsumeRequest;
import com.xuxiaojian.aipassagecreator.api.user.dto.QuotaIncreaseRequest;
import com.xuxiaojian.aipassagecreator.api.user.dto.UserPaymentProfileDTO;
import com.xuxiaojian.aipassagecreator.api.user.dto.VipUpdateRequest;
import com.xuxiaojian.aipassagecreator.common.BaseResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ai-user-service", path = "/api/internal/user")
public interface UserRemoteClient {

    @PostMapping("/quota/check-consume")
    BaseResponse<Boolean> checkAndConsumeQuota(@RequestBody QuotaConsumeRequest request);

    @PostMapping("/quota/increase")
    BaseResponse<Boolean> increaseQuota(@RequestBody QuotaIncreaseRequest request);

    @PostMapping("/vip/activate")
    BaseResponse<Boolean> activateVip(@RequestBody VipUpdateRequest request);

    @PostMapping("/vip/cancel")
    BaseResponse<Boolean> cancelVip(@RequestBody VipUpdateRequest request);

    @GetMapping("/statistics")
    BaseResponse<UserStatisticsDTO> getUserStatistics();

    @GetMapping("/profile/{userId}")
    BaseResponse<UserPaymentProfileDTO> getUserPaymentProfile(@PathVariable("userId") Long userId);
}
