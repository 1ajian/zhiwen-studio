package com.xuxiaojian.aipassagecreator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * ClassName: AsyncConfig
 * Package: com.xuxiaojian.aipassagecreator.config
 * Description:
 *
 * @Author 阿健
 * @Create 2026-05-27 23:35
 * @Version 1.0
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "articleExecutor")
    public Executor articleExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //核心线程数
        executor.setCorePoolSize(5);
        //最大线程数
        executor.setMaxPoolSize(10);
        //等待队列容量
        executor.setQueueCapacity(100);
        //空闲线程存活时间 本来还有时间单位
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("article-async-");
        //拒绝策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        //线程工厂
        executor.setThreadFactory(Executors.defaultThreadFactory());
        executor.setAwaitTerminationSeconds(60);
        //等待所有任务完成后关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }

    /**
     * 退款补偿异步线程池。
     * 退款补偿与文章生成分开，避免文章生成高峰拖慢支付补偿链路。
     *
     * @return 退款补偿线程池
     */
    @Bean(name = "refundCompensationTaskExecutor")
    public Executor refundCompensationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("refund-compensation-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setThreadFactory(Executors.defaultThreadFactory());
        executor.setAwaitTerminationSeconds(60);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }

    /**
     * 退款补偿延迟队列消费线程池。
     * 单线程即可按顺序阻塞读取 Redisson 队列，同时通过业务锁保证多实例幂等。
     *
     * @return 延迟队列消费线程池
     */
    @Bean(name = "refundCompensationDelayQueueExecutor")
    public Executor refundCompensationDelayQueueExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(10);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("refund-delay-queue-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setThreadFactory(Executors.defaultThreadFactory());
        executor.setAwaitTerminationSeconds(60);
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        return executor;
    }
}
