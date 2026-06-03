package com.xuxiaojian.aipassagecreator.config;

import com.xuxiaojian.aipassagecreator.publisher_listen.RefundCompensationRedisListener;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Redis 退款补偿监听配置。
 * 通过订阅固定 Channel 触发异步补偿，保持 handleRefund 主流程轻量返回。
 */
@Configuration
public class RedisRefundCompensationConfig {

    @Resource
    private RefundCompensationProperties refundCompensationProperties;

    @Bean
    public ChannelTopic refundCompensationTopic() {
        return new ChannelTopic(refundCompensationProperties.getChannel());
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory,
                                                                      RefundCompensationRedisListener refundCompensationRedisListener,
                                                                      ChannelTopic refundCompensationTopic) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(refundCompensationRedisListener, refundCompensationTopic);
        return container;
    }

}
