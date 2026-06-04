package com.xuxiaojian.aipassagecreator.model.vo;

import com.xuxiaojian.aipassagecreator.model.entity.AgentLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * ClassName: AgentExecutionStats
 * Package: com.xuxiaojian.aipassagecreator.model.vo
 * Description:
 *  智能体执行统计 VO
 * @Author 阿健
 * @Create 2026-06-03 19:13
 * @Version 1.0
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentExecutionStats implements Serializable {

    private static final long serialVersionUID = -2182504071860481158L;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 总耗时（毫秒）
     */
    private Integer totalDurationMs;

    /**
     * 智能体数量
     */
    private Integer agentCount;

    /**
     * 各智能体耗时（key: agentName, value: durationMs）
     */
    private Map<String, Integer> agentDurations;

    /**
     * 总体状态：SUCCESS（全部成功）、FAILED（存在失败）、RUNNING（执行中）
     */
    private String overallStatus;

    /**
     * 详细日志列表
     */
    private List<AgentLog> logs;
}
