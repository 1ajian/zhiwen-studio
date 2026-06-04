package com.xuxiaojian.aipassagecreator.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.xuxiaojian.aipassagecreator.constant.UserConstant;
import com.xuxiaojian.aipassagecreator.mapper.ArticleMapper;
import com.xuxiaojian.aipassagecreator.mapper.UserMapper;
import com.xuxiaojian.aipassagecreator.model.entity.Article;
import com.xuxiaojian.aipassagecreator.model.entity.User;
import com.xuxiaojian.aipassagecreator.model.enums.ArticleStatusEnum;
import com.xuxiaojian.aipassagecreator.model.vo.StatisticsVO;
import com.xuxiaojian.aipassagecreator.service.AgentLogService;
import com.xuxiaojian.aipassagecreator.service.StatisticsService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * ClassName: StatisticsServiceImpl
 * Package: com.xuxiaojian.aipassagecreator.service.impl
 * Description:
 *
 * @Author 阿健
 * @Create 2026-06-03 23:34
 * @Version 1.0
 */
@Service
@Slf4j
public class StatisticsServiceImpl implements StatisticsService {

    private static final String STATISTICS_CACHE_KEY = "statistics:overview";

    private static final long CACHE_EXPIRE_HOURS = 1L;

    @Resource
    private ArticleMapper articleMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private AgentLogService agentLogService;

    @Resource
    private RedisTemplate<String,Object> redisTemplate;

    @Override
    public StatisticsVO getStatistics() {
        //先从缓存中获取
        StatisticsVO cacheStats = (StatisticsVO) redisTemplate.opsForValue().get(STATISTICS_CACHE_KEY);
        if (cacheStats != null) {
            log.info("从缓存中获取统计数据");
            return cacheStats;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime localDateTimeBegin = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        //日创作数量
        Long todayCount = countArticlesByDataRange(localDateTimeBegin, LocalDateTime.now());

        //本周创建数量
        Long weekCount = countArticlesByDataRange(getWeekStart(),LocalDateTime.now());

        //本月创作数量
        Long monthCount = countArticlesByDataRange(getMonthStart(),LocalDateTime.now());

        //总创作数量
        Long totalCount = countTotalArticles();

        // 成功率统计
        Double successRate = calculateSuccessRate();

        //平均耗时统计
        Integer avgDurationMs = calculateAvgDuration();

        //本周活跃用户（有创作）
        Long activeUserCount  = countActiveUsers(getWeekStart());

        // 总用户数
        Long totalUserCount = countTotalUsers();

        // VIP 用户数
        Long vipUserCount = countVipUsers();

        // 配额使用情况（总配额 - 剩余配额）
        Long quotaUsed = calculateQuotaUsed();

        StatisticsVO statisticsVO = StatisticsVO.builder()
                .todayCount(todayCount)
                .weekCount(weekCount)
                .monthCount(monthCount)
                .totalCount(totalCount)
                .successRate(successRate)
                .avgDurationMs(avgDurationMs)
                .activeUserCount(activeUserCount)
                .totalUserCount(totalUserCount)
                .vipUserCount(vipUserCount)
                .quotaUsed(quotaUsed)
                .build();

        redisTemplate.opsForValue().set(STATISTICS_CACHE_KEY, statisticsVO, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        log.info("统计数据已缓存，过期时间: {} 小时", CACHE_EXPIRE_HOURS);

        return statisticsVO;
    }

    private Long calculateQuotaUsed() {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("userRole", UserConstant.DEFAULT_ROLE);
        List<User> users = userMapper.selectListByQuery(queryWrapper);
        long userCount = users.size();

        long remainQuota = (users.stream().mapToInt(user -> user.getQuota() != null ? user.getQuota() : 0).sum());

        return (userCount * UserConstant.DEFAULT_QUOTA) - remainQuota;
    }

    private Long countVipUsers() {
        QueryWrapper queryWrapper = QueryWrapper.create().eq("userRole", UserConstant.VIP_ROLE);
        long count = userMapper.selectCountByQuery(queryWrapper);
        return count;
    }

    private Long countTotalUsers() {
        return userMapper.selectCountByQuery(QueryWrapper.create());
    }

    private Long countActiveUsers(LocalDateTime weekStart) {
        QueryWrapper queryWrapper = QueryWrapper.create().ge("createTime", weekStart)
                .le("createTime", LocalDateTime.now());

        List<Article> articles = articleMapper.selectListByQuery(queryWrapper);
        if (CollUtil.isEmpty(articles)) {
            return 0L;
        }

        long count = articles.stream().map(Article::getUserId).distinct().count();

        return count;
    }

    private Integer calculateAvgDuration() {
        QueryWrapper queryWrapper = QueryWrapper.create().eq("status", ArticleStatusEnum.COMPLETED.getValue())
                .isNotNull("completedTime");

        List<Article> articles = articleMapper.selectListByQuery(queryWrapper);

        if (CollUtil.isEmpty(articles)) {
            return 0;
        }

        double avg = articles.stream().filter(article -> article.getCreateTime() != null && article.getCompletedTime() != null)
                .mapToLong(
                        article -> {
                            long createMillis = Timestamp.valueOf(article.getCreateTime()).getTime();
                            long completedMillis = Timestamp.valueOf(article.getCompletedTime()).getTime();
                            return completedMillis - createMillis;
                        }
                ).average().orElse(0.0);
        return (int)avg;
    }

    private Double calculateSuccessRate() {
        Long totalCount = countTotalArticles();
        if (totalCount == 0) {
            return 0.0;
        }

        QueryWrapper queryWrapper = QueryWrapper.create().eq("status", ArticleStatusEnum.COMPLETED.getValue());
        long successCount = articleMapper.selectCountByQuery(queryWrapper);
        return ((double) successCount / totalCount.doubleValue()) * 100;
    }

    private LocalDateTime getMonthStart() {
        LocalDate now = LocalDate.now();
        int dayOfMonth = now.getDayOfMonth();
        LocalDate firstLocalDate = now.minusDays(dayOfMonth - 1);
        return LocalDateTime.of(firstLocalDate, LocalTime.MIN);
    }

    private Long countTotalArticles() {
        return articleMapper.selectCountByQuery(QueryWrapper.create());
    }

    private LocalDateTime getWeekStart() {
        LocalDate today = LocalDate.now();
        LocalDateTime mondy = LocalDateTime.of(today.minusDays(today.getDayOfWeek().getValue() - 1), LocalTime.MIN);
        return mondy;
    }

    private Long countArticlesByDataRange(LocalDateTime localDateTimeBegin, LocalDateTime now) {
        QueryWrapper queryWrapper = new QueryWrapper().ge("createTime", localDateTimeBegin).le("createTime", now);
        return articleMapper.selectCountByQuery(queryWrapper);
    }


}
