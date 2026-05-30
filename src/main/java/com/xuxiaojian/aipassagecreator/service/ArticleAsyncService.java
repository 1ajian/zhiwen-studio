package com.xuxiaojian.aipassagecreator.service;

import com.xuxiaojian.aipassagecreator.manager.SseEmitterManager;
import com.xuxiaojian.aipassagecreator.model.dto.article.ArticleState;
import com.xuxiaojian.aipassagecreator.model.enums.ArticleStatusEnum;
import com.xuxiaojian.aipassagecreator.model.enums.SseMessageTypeEnum;
import com.xuxiaojian.aipassagecreator.utils.GsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ClassName: ArticleAsyncService
 * Package: com.xuxiaojian.aipassagecreator.service.impl
 * Description:
 *
 * @Author 阿健
 * @Create 2026-05-27 23:48
 * @Version 1.0
 */
@Service
@Slf4j
public class ArticleAsyncService {

    @Resource
    private ArticleAgentService articleAgentService;

    @Resource
    private SseEmitterManager sseEmitterManager;

    @Resource
    private ArticleService articleService;

    @Async("articleExecutor")
    public void executeArticleGeneration(String taskId, String topic, String style, List<String> enabledImageMethods) {
        log.info("异步任务开始,taskId={},topic={}",taskId,topic);

        try {
            articleService.updateArticleStatus(taskId, ArticleStatusEnum.PROCESSING,null);

            ArticleState state = new ArticleState();
            state.setTaskId(taskId);
            state.setTopic(topic);
            state.setStyle(style);
            state.setEnabledImageMethods(enabledImageMethods);

            //执行智能体异步编排，并通过SSE推送进度
            articleAgentService.executeArticleGeneration(state,message -> {
                handleAgentMessage(taskId,message,state);
            });

            //保存完整文章到数据库
            articleService.saveArticleContent(taskId,state);

            //更新状态为已完成
            articleService.updateArticleStatus(taskId,ArticleStatusEnum.COMPLETED,null);

            //推送完成消息
            sendSseMessage(taskId, SseMessageTypeEnum.ALL_COMPLETE, Map.of("taskId",taskId));

            //完成SSE 连接
            sseEmitterManager.complete(taskId);

            log.info("异步任务完成,taskId={}",taskId);
        }catch (Exception e) {
            log.error("异步任务失败,taskId={}",taskId,e);
            //更新状态为失败
            articleService.updateArticleStatus(taskId,ArticleStatusEnum.FAILED,e.getMessage());
            //推送错误消息
            sendSseMessage(taskId,SseMessageTypeEnum.ERROR,Map.of("message",e.getMessage()));
            //完成SSE连接
            sseEmitterManager.complete(taskId);
        }
    }

    /**
     * 处理智能体消息并推送
     * @param taskId
     * @param message
     * @param state
     */
    private void handleAgentMessage(String taskId, String message, ArticleState state) {
        Map<String,Object> data = buildMessageData(message,state);
        if (data != null) {
            sseEmitterManager.send(taskId, GsonUtils.toJson(data));
        }
    }

    private Map<String, Object> buildMessageData(String message, ArticleState state) {
        // 处理流式消息(带冒号分割符)
        String streamingPrefix2 = SseMessageTypeEnum.AGENT2_STREAMING.getStreamingPrefix();
        String streamingPrefix3 = SseMessageTypeEnum.AGENT3_STREAMING.getStreamingPrefix();
        String imageCompletePrefix = SseMessageTypeEnum.IMAGE_COMPLETE.getStreamingPrefix();

        if (message.startsWith(streamingPrefix2)) {
            return buildStreamingData(SseMessageTypeEnum.AGENT2_STREAMING,message.substring(streamingPrefix2.length()));
        }

        if (message.startsWith(streamingPrefix3)) {
            return buildStreamingData(SseMessageTypeEnum.AGENT3_STREAMING,message.substring(streamingPrefix3.length()));
        }

        if (message.startsWith(imageCompletePrefix)) {
            String imageJson = message.substring(imageCompletePrefix.length());
            return buildImageCompleteData(imageJson);
        }
        
        //处理完成消息（枚举值含有XXX_COMPLETE,除了单张图片完成）
        return buildCompleteMessageData(message,state);
    }

    private Map<String, Object> buildCompleteMessageData(String message, ArticleState state) {
        HashMap<String, Object> data = new HashMap<>();
        if (SseMessageTypeEnum.AGENT1_COMPLETE.getValue().equals(message)) {
            data.put("type",SseMessageTypeEnum.AGENT1_COMPLETE.getValue());
            data.put("title",state.getTitle());
        } else if (SseMessageTypeEnum.AGENT2_COMPLETE.getValue().equals(message)) {
            data.put("type",SseMessageTypeEnum.AGENT2_COMPLETE.getValue());
            data.put("outline",state.getOutline());
        } else if (SseMessageTypeEnum.AGENT3_COMPLETE.getValue().equals(message)) {
            data.put("type",SseMessageTypeEnum.AGENT3_COMPLETE.getValue());
        } else if (SseMessageTypeEnum.AGENT4_COMPLETE.getValue().equals(message)) {
            data.put("type",SseMessageTypeEnum.AGENT4_COMPLETE.getValue());
            data.put("imageRequirements",state.getImageRequirements());
        } else if (SseMessageTypeEnum.AGENT5_COMPLETE.getValue().equals(message)) {
            data.put("type",SseMessageTypeEnum.AGENT5_COMPLETE.getValue());
            data.put("images",state.getImages());
        } else if (SseMessageTypeEnum.MERGE_COMPLETE.getValue().equals(message)) {
            data.put("type",SseMessageTypeEnum.MERGE_COMPLETE.getValue());
            data.put("fullContent",state.getFullContent());
        } else {
            return null;
        }

        return data;
    }

    /**
     * 构建图片完成数据
     * @param imageJson
     * @return
     */
    private Map<String, Object> buildImageCompleteData(String imageJson) {
        HashMap<String, Object> data = new HashMap<>();
        data.put("type",SseMessageTypeEnum.IMAGE_COMPLETE.getValue());
        data.put("image",GsonUtils.fromJson(imageJson, ArticleState.ImageResult.class));
        return data;
    }

    /**
     * 构建流式输出数据
     * @param sseMessageTypeEnum
     * @param content
     * @return
     */
    private Map<String, Object> buildStreamingData(SseMessageTypeEnum sseMessageTypeEnum, String content) {
        HashMap<String, Object> data = new HashMap<>();
        data.put("type",sseMessageTypeEnum.getValue());
        data.put("content",content);
        return data;
    }

    /**
     * 发送SSE 消息
     * @param taskId
     * @param type
     * @param additionalData
     */
    private void sendSseMessage(String taskId, SseMessageTypeEnum type, Map<String, String> additionalData) {
        HashMap<String, Object> data = new HashMap<>();
        data.put("type",type.getValue());
        data.putAll(additionalData);
        sseEmitterManager.send(taskId,GsonUtils.toJson(data));
    }
}
