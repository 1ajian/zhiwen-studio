package com.xuxiaojian.aipassagecreator.publisher_listen;

import com.xuxiaojian.aipassagecreator.publisher_listen.process.RefundCompensationService;
import jakarta.annotation.Resource;
import org.jspecify.annotations.Nullable;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * ClassName: RefundCompensationRedisListener
 * Package: com.xuxiaojian.aipassagecreator.listen
 * Description:
 *   Redis 消息监听器。
 *   这里只负责解析 eventId 并委派到补偿服务
 * @Author 阿健
 * @Create 2026-06-03 14:24
 * @Version 1.0
 */
@Component
public class RefundCompensationRedisListener implements MessageListener {
    @Resource
    private RefundCompensationService refundCompensationService;

    /**
     * 监听消息,处理事件
     * @param message
     * @param pattern
     */
    @Override
    public void onMessage(Message message, @Nullable byte[] pattern) {
        String eventId = new String(message.getBody(), StandardCharsets.UTF_8);
        refundCompensationService.handleEvent(eventId);
    }
}
