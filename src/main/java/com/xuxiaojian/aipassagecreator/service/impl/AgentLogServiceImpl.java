package com.xuxiaojian.aipassagecreator.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.xuxiaojian.aipassagecreator.mapper.AgentLogMapper;
import com.xuxiaojian.aipassagecreator.model.entity.AgentLog;
import com.xuxiaojian.aipassagecreator.model.vo.AgentExecutionStats;
import com.xuxiaojian.aipassagecreator.service.AgentLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ClassName: AgentLogServiceImpl
 * Package: com.xuxiaojian.aipassagecreator.service.impl
 * Description:
 *
 * @Author 阿健
 * @Create 2026-06-03 19:24
 * @Version 1.0
 */
@Service
@Slf4j
public class AgentLogServiceImpl extends ServiceImpl<AgentLogMapper, AgentLog> implements AgentLogService {

    @Async("refundCompensationTaskExecutor")
    @Override
    public void saveLogAsync(AgentLog agentLog) {
        try {
            this.save(agentLog);
            log.info("智能体日志已保存,taskId = {},agentName = {},status = {},durationMs = {}",
                    agentLog.getTaskId(),
                    agentLog.getAgentName(),
                    agentLog.getStatus(),
                    agentLog.getDurationMs());
        } catch (Exception e) {
            log.error("保存智能体日志失败,taskId = {},agentName = {}",agentLog.getTaskId(),agentLog.getAgentName(),e);
        }

    }

    @Override
    public List<AgentLog> getLogsByTaskId(String taskId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("taskId", taskId)
                .orderBy("createTime", true);
        return this.list(queryWrapper);
    }


    @Override
    public AgentExecutionStats getExecutionStats(String taskId) {
        List<AgentLog> logs = getLogsByTaskId(taskId);
        if (logs == null || logs.isEmpty()) {
            return AgentExecutionStats.builder()
                    .taskId(taskId)
                    .agentCount(0)
                    .totalDurationMs(0)
                    .overallStatus("NOT_FOUND")
                    .build();
        }

        // 计算统计数据
        int totalDuration = 0;
        Map<String, Integer> agentDurations = new HashMap<>();
        String overallStatus = "SUCCESS";

        agentDurations = logs.stream().collect(Collectors.groupingBy(AgentLog::getAgentName,
                Collectors.summingInt(AgentLog::getDurationMs)));

        totalDuration = agentDurations.values().stream().mapToInt(Integer::intValue).sum();

        for (AgentLog agentLog : logs) {
            if ("FAILED".equals(agentLog.getStatus())) {
                overallStatus = "FAILED";
            } else if ("RUNNING".equals(agentLog.getStatus()) && !"FAILED".equals(overallStatus)) {
                overallStatus = "RUNNING";
            }
        }

        return AgentExecutionStats.builder()
                .taskId(taskId)
                .totalDurationMs(totalDuration)
                .agentCount(logs.size())
                .agentDurations(agentDurations)
                .overallStatus(overallStatus)
                .logs(logs)
                .build();

    }
}
