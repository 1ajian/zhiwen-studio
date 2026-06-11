package com.xuxiaojian.aipassagecreator.api.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleStatisticsDTO implements Serializable {

    private static final long serialVersionUID = 2026061007L;

    private Long todayCount;

    private Long weekCount;

    private Long monthCount;

    private Long totalCount;

    private Double successRate;

    private Integer avgDurationMs;

    private Long activeUserCount;
}
