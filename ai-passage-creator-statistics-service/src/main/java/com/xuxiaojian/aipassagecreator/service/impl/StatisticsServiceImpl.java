package com.xuxiaojian.aipassagecreator.service.impl;

import com.xuxiaojian.aipassagecreator.api.article.ArticleRemoteClient;
import com.xuxiaojian.aipassagecreator.api.payment.PaymentRemoteClient;
import com.xuxiaojian.aipassagecreator.api.statistics.dto.ArticleStatisticsDTO;
import com.xuxiaojian.aipassagecreator.api.statistics.dto.PaymentStatisticsDTO;
import com.xuxiaojian.aipassagecreator.api.statistics.dto.UserStatisticsDTO;
import com.xuxiaojian.aipassagecreator.api.user.UserRemoteClient;
import com.xuxiaojian.aipassagecreator.common.BaseResponse;
import com.xuxiaojian.aipassagecreator.exception.BusinessException;
import com.xuxiaojian.aipassagecreator.exception.ErrorCode;
import com.xuxiaojian.aipassagecreator.model.vo.StatisticsVO;
import com.xuxiaojian.aipassagecreator.service.StatisticsService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 统计服务实现。
 * 统计数据通过 Feign 从用户、文章、支付服务聚合，避免本服务直连业务库。
 */
@Service
@Slf4j
public class StatisticsServiceImpl implements StatisticsService {

    private static final String STATISTICS_CACHE_KEY = "statistics:overview";

    private static final long CACHE_EXPIRE_HOURS = 1L;

    @Resource
    private ArticleRemoteClient articleRemoteClient;

    @Resource
    private UserRemoteClient userRemoteClient;

    @Resource
    private PaymentRemoteClient paymentRemoteClient;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public StatisticsVO getStatistics() {
        Object cache = redisTemplate.opsForValue().get(STATISTICS_CACHE_KEY);
        if (cache instanceof StatisticsVO statisticsVO) {
            return statisticsVO;
        }

        ArticleStatisticsDTO articleStatistics = requireData(articleRemoteClient.getArticleStatistics(), "文章统计获取失败");
        UserStatisticsDTO userStatistics = requireData(userRemoteClient.getUserStatistics(), "用户统计获取失败");
        PaymentStatisticsDTO paymentStatistics = requireData(paymentRemoteClient.getPaymentStatistics(), "支付统计获取失败");
        log.debug("支付统计已聚合,total={},success={},refund={}",
                paymentStatistics.getTotalPaymentCount(),
                paymentStatistics.getSucceededPaymentCount(),
                paymentStatistics.getRefundedPaymentCount());

        StatisticsVO statisticsVO = StatisticsVO.builder()
                .todayCount(articleStatistics.getTodayCount())
                .weekCount(articleStatistics.getWeekCount())
                .monthCount(articleStatistics.getMonthCount())
                .totalCount(articleStatistics.getTotalCount())
                .successRate(articleStatistics.getSuccessRate())
                .avgDurationMs(articleStatistics.getAvgDurationMs())
                .activeUserCount(articleStatistics.getActiveUserCount())
                .totalUserCount(userStatistics.getTotalUserCount())
                .vipUserCount(userStatistics.getVipUserCount())
                .quotaUsed(userStatistics.getQuotaUsed())
                .build();

        redisTemplate.opsForValue().set(STATISTICS_CACHE_KEY, statisticsVO, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        return statisticsVO;
    }

    private <T> T requireData(BaseResponse<T> response, String message) {
        if (response == null || response.getCode() != 0 || response.getData() == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, message);
        }
        return response.getData();
    }
}
