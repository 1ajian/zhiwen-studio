package com.xuxiaojian.aipassagecreator.aop;

import com.xuxiaojian.aipassagecreator.annotation.AgentExecution;
import com.xuxiaojian.aipassagecreator.model.dto.article.ArticleState;
import com.xuxiaojian.aipassagecreator.model.entity.AgentLog;
import com.xuxiaojian.aipassagecreator.service.AgentLogService;
import com.xuxiaojian.aipassagecreator.utils.GsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ClassName: AgentExecutionAspect
 * Package: com.xuxiaojian.aipassagecreator.aop
 * Description:
 *
 * @Author 阿健
 * @Create 2026-06-03 19:20
 * @Version 1.0
 */
@Aspect
@Component
@Slf4j
public class AgentExecutionAspect {

    @Resource
    private AgentLogService agentLogService;

    @Around("@annotation(agentExecution)")
    public Object aroundAgentExecution(ProceedingJoinPoint joinPoint, AgentExecution agentExecution) throws Throwable {
        long startTime = System.currentTimeMillis();
        LocalDateTime startDateTime = LocalDateTime.now();

        //提取 taskId 和 输入数据
        String taskId = extractTaskId(joinPoint);
        String inputData = extractInputData(joinPoint);
        String prompt = extractPrompt(joinPoint);

        //创建日志对象
        AgentLog agentLog = AgentLog.builder()
                .taskId(taskId)
                .agentName(agentExecution.value())
                .startTime(startDateTime)
                .status("RUNNING")
                .prompt(prompt)
                .inputData(inputData)
                .build();

        Object result = null;
        try {
            //执行目标方法
            result = joinPoint.proceed();

            // 记录成功状态
            agentLog.setStatus("SUCCESS");
            agentLog.setEndTime(LocalDateTime.now());
            agentLog.setDurationMs((int) (System.currentTimeMillis() - startTime));
            agentLog.setOutputData(extractOutputData(result));

            log.info("智能体执行成功: {},taskId={},耗时={}ms",agentExecution.value(),taskId,agentLog.getDurationMs());
        }catch (Throwable e) {
            //记录失败状态
            agentLog.setStatus("FAILED");
            agentLog.setEndTime(LocalDateTime.now());
            agentLog.setDurationMs((int) (System.currentTimeMillis() - startTime ));
            agentLog.setErrorMessage(e.getMessage());
            log.error("智能体执行失败: {},taskId = {},错误={}",agentExecution.value(),taskId,e.getMessage(),e);
            throw e;
        } finally {
            agentLogService.saveLogAsync(agentLog);
        }
        return result;
    }

    /**
     * 提取输出数据
     * @param result
     * @return
     */
    private String extractOutputData(Object result) {
        try {
            if (result == null) {
                return null;
            }

            //只记录简单类型，避免数据过大
            if (result instanceof String || result instanceof Number || result instanceof Boolean) {
                return String.valueOf(result);
            }

            // 对于集合类型,记录数量
            if (result instanceof List) {
                return "{\"listSize\": " + ((List<?>) result).size() + "}";
            }

            return "{\"type\": \"" + result.getClass().getSimpleName() + "\"}";
        }catch (Exception e) {
            log.warn("提取输出数据失败",e);
            return null;
        }
    }

    private String extractPrompt(ProceedingJoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            return method.getDeclaringClass().getSimpleName() + "." + method.getName();
        }catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取输入数据（只记录关键信息）
     * @param joinPoint
     * @return
     */
    private String extractInputData(ProceedingJoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args == null || args.length == 0) {
                return null;
            }

            Map<String, Object> inputMap = new HashMap<>();
            MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = methodSignature.getParameterNames();
            for (int i = 0; i < args.length && i < paramNames.length; i++) {
                Object arg = args[i];
                // 只记录基本类型和简单对象，避免数据过大
                if (arg instanceof String || arg instanceof Number || arg instanceof Boolean) {
                    inputMap.put(paramNames[i],arg);
                } else if (arg instanceof ArticleState) {
                    ArticleState state = (ArticleState) arg;
                    inputMap.put("taskId",state.getTaskId());
                    if (state.getTitle() != null) {
                        inputMap.put("mainTitle",state.getTitle().getMainTitle());
                    }
                }
            }

            return inputMap.isEmpty() ? null : GsonUtils.toJson(inputMap);
        }catch (Exception e) {
            log.warn("提取输入数据失败",e);
            return null;
        }
    }

    private String extractTaskId(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return "unknown";
        }

        for (Object arg : args) {
            if (arg instanceof ArticleState) {
                return ((ArticleState) arg).getTaskId();
            }
        }

        //尝试从第一个String 参数获取（可能是taskId）
        for (Object arg : args) {
            if (arg instanceof String) {
                return (String) arg;
            }
        }
        return "unknown";
    }


}
