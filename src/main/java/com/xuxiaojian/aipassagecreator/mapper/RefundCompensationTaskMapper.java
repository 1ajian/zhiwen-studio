package com.xuxiaojian.aipassagecreator.mapper;

import com.mybatisflex.core.BaseMapper;
import com.xuxiaojian.aipassagecreator.model.entity.RefundCompensationTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 退款补偿失败任务 Mapper。
 * 这里提供抢占处理状态的更新语句，避免定时补偿并发执行同一条任务。
 */
@Mapper
public interface RefundCompensationTaskMapper extends BaseMapper<RefundCompensationTask> {

    @Select("SELECT * FROM refund_compensation_task WHERE eventId = #{eventId} LIMIT 1")
    RefundCompensationTask selectByEventId(@Param("eventId") String eventId);

    @Update("UPDATE refund_compensation_task " +
            "SET status = 'PROCESSING', processingStartTime = #{processingStartTime}, updateTime = #{updateTime} " +
            "WHERE id = #{id} AND status IN ('PENDING', 'FAILED')")
    int markProcessing(@Param("id") Long id,
                       @Param("processingStartTime") LocalDateTime processingStartTime,
                       @Param("updateTime") LocalDateTime updateTime);

    @Update("UPDATE refund_compensation_task " +
            "SET status = 'SUCCESS', processingStartTime = NULL, lastError = NULL, updateTime = #{updateTime} " +
            "WHERE id = #{id}")
    int markSuccess(@Param("id") Long id, @Param("updateTime") LocalDateTime updateTime);

    @Update("UPDATE refund_compensation_task " +
            "SET status = #{status}, retryCount = #{retryCount}, nextRetryTime = #{nextRetryTime}, " +
            "lastError = #{lastError}, processingStartTime = NULL, updateTime = #{updateTime} " +
            "WHERE id = #{id}")
    int markRetryResult(@Param("id") Long id,
                        @Param("status") String status,
                        @Param("retryCount") Integer retryCount,
                        @Param("nextRetryTime") LocalDateTime nextRetryTime,
                        @Param("lastError") String lastError,
                        @Param("updateTime") LocalDateTime updateTime);

    @Update("UPDATE refund_compensation_task " +
            "SET status = 'FAILED', processingStartTime = NULL, updateTime = #{updateTime}, lastError = 'PROCESSING超时回退' " +
            "WHERE status = 'PROCESSING' AND processingStartTime < #{threshold}")
    int resetExpiredProcessingTasks(@Param("threshold") LocalDateTime threshold,
                                    @Param("updateTime") LocalDateTime updateTime);
}
