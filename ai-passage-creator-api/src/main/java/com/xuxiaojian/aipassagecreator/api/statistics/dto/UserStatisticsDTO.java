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
public class UserStatisticsDTO implements Serializable {

    private static final long serialVersionUID = 2026061006L;

    private Long totalUserCount;

    private Long vipUserCount;

    private Long quotaUsed;
}
