package com.xuxiaojian.aipassagecreator.controller;

import cn.hutool.core.collection.CollUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.xuxiaojian.aipassagecreator.api.statistics.dto.ArticleStatisticsDTO;
import com.xuxiaojian.aipassagecreator.common.BaseResponse;
import com.xuxiaojian.aipassagecreator.common.ResultUtils;
import com.xuxiaojian.aipassagecreator.mapper.ArticleMapper;
import com.xuxiaojian.aipassagecreator.model.entity.Article;
import com.xuxiaojian.aipassagecreator.model.enums.ArticleStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 文章服务内部统计接口。
 * 统计服务通过 Feign 访问该接口，避免直接依赖文章库 Mapper。
 */
@RestController
@RequestMapping("/internal/article")
public class InternalArticleController {

    @Resource
    private ArticleMapper articleMapper;

    @GetMapping("/statistics")
    public BaseResponse<ArticleStatisticsDTO> getArticleStatistics() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime weekStart = LocalDateTime.of(LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1), LocalTime.MIN);
        LocalDateTime monthStart = LocalDateTime.of(LocalDate.now().withDayOfMonth(1), LocalTime.MIN);

        ArticleStatisticsDTO statistics = ArticleStatisticsDTO.builder()
                .todayCount(countArticlesByDataRange(todayStart, now))
                .weekCount(countArticlesByDataRange(weekStart, now))
                .monthCount(countArticlesByDataRange(monthStart, now))
                .totalCount(articleMapper.selectCountByQuery(QueryWrapper.create()))
                .successRate(calculateSuccessRate())
                .avgDurationMs(calculateAvgDuration())
                .activeUserCount(countActiveUsers(weekStart))
                .build();
        return ResultUtils.success(statistics);
    }

    private Long countArticlesByDataRange(LocalDateTime begin, LocalDateTime end) {
        QueryWrapper queryWrapper = QueryWrapper.create().ge("createTime", begin).le("createTime", end);
        return articleMapper.selectCountByQuery(queryWrapper);
    }

    private Long countActiveUsers(LocalDateTime weekStart) {
        QueryWrapper queryWrapper = QueryWrapper.create().ge("createTime", weekStart).le("createTime", LocalDateTime.now());
        List<Article> articles = articleMapper.selectListByQuery(queryWrapper);
        if (CollUtil.isEmpty(articles)) {
            return 0L;
        }
        return articles.stream().map(Article::getUserId).distinct().count();
    }

    private Integer calculateAvgDuration() {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("status", ArticleStatusEnum.COMPLETED.getValue())
                .isNotNull("completedTime");
        List<Article> articles = articleMapper.selectListByQuery(queryWrapper);
        if (CollUtil.isEmpty(articles)) {
            return 0;
        }
        double avg = articles.stream()
                .filter(article -> article.getCreateTime() != null && article.getCompletedTime() != null)
                .mapToLong(article -> Timestamp.valueOf(article.getCompletedTime()).getTime()
                        - Timestamp.valueOf(article.getCreateTime()).getTime())
                .average()
                .orElse(0.0);
        return (int) avg;
    }

    private Double calculateSuccessRate() {
        Long totalCount = articleMapper.selectCountByQuery(QueryWrapper.create());
        if (totalCount == 0) {
            return 0.0;
        }
        long successCount = articleMapper.selectCountByQuery(
                QueryWrapper.create().eq("status", ArticleStatusEnum.COMPLETED.getValue()));
        return ((double) successCount / totalCount.doubleValue()) * 100;
    }
}
