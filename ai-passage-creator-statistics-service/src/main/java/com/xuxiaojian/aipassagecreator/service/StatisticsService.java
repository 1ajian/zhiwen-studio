package com.xuxiaojian.aipassagecreator.service;

import com.xuxiaojian.aipassagecreator.model.vo.StatisticsVO;

/**
 * ClassName: StatisticsService
 * Package: com.xuxiaojian.aipassagecreator.service
 * Description:
 *
 * @Author 阿健
 * @Create 2026-06-03 23:33
 * @Version 1.0
 */
public interface StatisticsService {

    /**
     * 获取系统统计数据
     * @return
     */
    StatisticsVO getStatistics();
}
