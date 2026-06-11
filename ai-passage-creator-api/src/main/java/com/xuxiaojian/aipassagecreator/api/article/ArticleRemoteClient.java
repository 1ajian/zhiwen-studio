package com.xuxiaojian.aipassagecreator.api.article;

import com.xuxiaojian.aipassagecreator.api.statistics.dto.ArticleStatisticsDTO;
import com.xuxiaojian.aipassagecreator.common.BaseResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "ai-article-service", path = "/api/internal/article")
public interface ArticleRemoteClient {

    @GetMapping("/statistics")
    BaseResponse<ArticleStatisticsDTO> getArticleStatistics();
}
