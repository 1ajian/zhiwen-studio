package com.xuxiaojian.aipassagecreator.manager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.xuxiaojian.aipassagecreator.constant.ArticleConstant.SSE_RECONNECT_TIME_MS;
import static com.xuxiaojian.aipassagecreator.constant.ArticleConstant.SSE_TIMEOUT_MS;

/**
 * ClassName: SseEmitterManager
 * Package: com.xuxiaojian.aipassagecreator.manager
 * Description:
 *
 * @Author 阿健
 * @Create 2026-05-27 23:03
 * @Version 1.0
 */
@Component
@Slf4j
public class SseEmitterManager {

    /**
     * 存储所有的 SseEmitter
     */
    private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();

    /**
     * 创建 SseEmitter
     * @param taskId 任务ID
     * @return
     */
    public SseEmitter createEmitter(String taskId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        //设置超时回调
        emitter.onTimeout(() -> {
            log.warn("SSE 连接超时,taskId={}",taskId);
            emitterMap.remove(taskId);
        });

        //设置完成回调
        emitter.onCompletion(() -> {
            log.info("SSE 连接完成,taskId={}",taskId);
            emitterMap.remove(taskId);
        });

        //设置错误回调
        emitter.onError((e) -> {
            log.error("SSE 连接错误,taskId={}",taskId,e);
            emitterMap.remove(taskId);
        });

        emitterMap.put(taskId,emitter);
        log.info("SSE 连接已创建,taskId={}",taskId);

        return emitter;
    }

    /**
     * 发送消息
     * @param taskId
     * @param message
     */
    public void send(String taskId,String message) {
        SseEmitter emitter = emitterMap.get(taskId);
        if (emitter == null) {
            log.warn("SSE Emitter 不存在,taskId={}",taskId);
            return;
        }

        try {
            emitter.send(SseEmitter.event().data(message).reconnectTime(SSE_RECONNECT_TIME_MS));
            log.debug("SSE 消息发送成功,taskId={},message={}",taskId,message);
        }catch (Exception e) {
            log.error("SSE 消息发送失败,taskId={}",taskId,e);
            emitterMap.remove(taskId);
        }
    }

    /**
     * 完成连接
     * @param taskId
     */
    public void complete(String taskId) {
        SseEmitter emitter = emitterMap.get(taskId);
        if (emitter == null) {
            log.warn("SSE Emitter 不存在,taskId = {}",taskId);
            return;
        }

        try {
            emitter.complete();
            log.info("SSE 连接已完成，taskId={}",taskId);
        }catch (Exception e) {
            log.error("SSE 连接完成失败,taskId={}",taskId,e);
        }finally {
            emitterMap.remove(taskId);
        }
    }

    /**
     * 检查 Emitter 是否存在
     * @param taskId
     * @return
     */
    public boolean exists(String taskId) {
        return emitterMap.containsKey(taskId);
    }
}
