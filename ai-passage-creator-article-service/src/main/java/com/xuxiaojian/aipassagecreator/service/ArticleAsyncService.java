package com.xuxiaojian.aipassagecreator.service;

import com.google.gson.reflect.TypeToken;
import com.xuxiaojian.aipassagecreator.agent.ArticleAgentOrchestrator;
import com.xuxiaojian.aipassagecreator.agent.config.AgentConfig;
import com.xuxiaojian.aipassagecreator.exception.BusinessException;
import com.xuxiaojian.aipassagecreator.exception.ErrorCode;
import com.xuxiaojian.aipassagecreator.manager.SseEmitterManager;
import com.xuxiaojian.aipassagecreator.model.dto.article.ArticleState;
import com.xuxiaojian.aipassagecreator.model.entity.Article;
import com.xuxiaojian.aipassagecreator.model.enums.ArticlePhaseEnum;
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

    @Resource
    private ArticleAgentOrchestrator articleAgentOrchestrator;

    @Resource
    private AgentConfig agentConfig;

    /**
     * 阶段1：异步生成标题方案
     * @param taskId
     * @param topic
     * @param style
     */
    @Async("articleExecutor")
    public void executePhase1(String taskId,String topic,String style) {

        log.info("阶段1异步任务开始,taskId = {},topic = {},style = {}",taskId,topic,style);
        boolean orchestratorEnabled = agentConfig.isOrchestratorEnabled();

        try {
            //更新状态和阶段
            articleService.updateArticleStatus(taskId,ArticleStatusEnum.PROCESSING,null);
            articleService.updatePhase(taskId, ArticlePhaseEnum.TITLE_GENERATING);

            //创建状态对象
            ArticleState state = new ArticleState();
            state.setTaskId(taskId);
            state.setTopic(topic);
            state.setStyle(style);

            if (orchestratorEnabled) {
                articleAgentOrchestrator.executePhase1_GenerateTitles(state,message -> {
                    handleAgentMessage(taskId,message,state);
                });
            } else {
                //执行阶段1：生成标题方案
                articleAgentService.executePhase1_GenerateTitles(state,message -> {
                    handleAgentMessage(taskId,message,state);
                });
            }

            //保存标题方案到数据库
            articleService.saveTitleOptions(taskId,state.getTitleOptions());

            //更新阶段为等待选择标题
            articleService.updatePhase(taskId,ArticlePhaseEnum.TITLE_SELECTING);

            //推送标题方案生成完成消息
            Map<String, Object> data = new HashMap<>();
            data.put("titleOptions",state.getTitleOptions());
            sendSseMessage(taskId,SseMessageTypeEnum.TITLES_GENERATED,data);

            log.info("阶段1异步任务完成,taskId={}",taskId);
        }catch (Exception e) {
            log.error("阶段1异步任务失败,taskId={}",taskId,e);
            articleService.updateArticleStatus(taskId,ArticleStatusEnum.FAILED,e.getMessage());
            //推送错误信息
            sendSseMessage(taskId,SseMessageTypeEnum.ERROR,Map.of("message",e.getMessage()));
            //完成SSE连接
            sseEmitterManager.complete(taskId);
        }
    }

    /**
     * 阶段2：异步生成大纲（用户确认标题后调用）
     * @param taskId
     */
    @Async("articleExecutor")
    public void executePhase2(String taskId) {
        log.info("阶段2异步任务开始,taskId = {}",taskId);
        boolean orchestratorEnabled = agentConfig.isOrchestratorEnabled();

        try {
            Article article = articleService.getByTaskId(taskId);
            if (article == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"文章不存在");
            }
            //创建状态对象
            ArticleState state = new ArticleState();
            state.setTaskId(taskId);
            state.setStyle(article.getStyle());
            state.setUserDescription(article.getUserDescription());

            //设置标题
            ArticleState.TitleResult title = new ArticleState.TitleResult();
            title.setMainTitle(article.getMainTitle());
            title.setSubTitle(article.getSubTitle());
            state.setTitle(title);

            if (orchestratorEnabled) {
                articleAgentOrchestrator.executePhase2_GenerateOutline(state,message -> {
                    handleAgentMessage(taskId,message,state);
                });
            } else {
                // 执行阶段2：生成大纲
                articleAgentService.execute2_GenerateOutline(state,message -> {
                    handleAgentMessage(taskId,message,state);
                });
            }


            //保存大纲到数据库
            article = articleService.getByTaskId(taskId);
            article.setOutline(GsonUtils.toJson(state.getOutline().getSections()));
            articleService.updateById(article);

            //更新阶段为等待编辑大纲
            articleService.updatePhase(taskId,ArticlePhaseEnum.OUTLINE_EDITING);

            //推送大纲生成完成消息
            Map<String, Object> data = new HashMap<>();
            data.put("outline",state.getOutline().getSections());
            sendSseMessage(taskId,SseMessageTypeEnum.OUTLINE_GENERATED,data);

            log.info("阶段2异步任务完成,taskId = {}",taskId);
        }catch (Exception e) {
            log.info("阶段2异步任务失败,taskId = {}",taskId,e);
            articleService.updateArticleStatus(taskId,ArticleStatusEnum.FAILED,e.getMessage());
            sendSseMessage(taskId,SseMessageTypeEnum.ERROR,Map.of("message",e.getMessage()));
            sseEmitterManager.complete(taskId);
        }

    }

    /**
     * 阶段3：异步生成正文+配图（用户确认大纲后调用）
     * @param taskId
     */
    @Async("articleExecutor")
    public void executePhase3(String taskId) {
        log.info("阶段3异步任务开始,taskId = {}",taskId);
        boolean orchestratorEnabled = agentConfig.isOrchestratorEnabled();


        try {
            Article article = articleService.getByTaskId(taskId);
            if (article == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"文章不存在");
            }
            //创建状态对象
            ArticleState state = new ArticleState();
            state.setTaskId(taskId);
            state.setStyle(article.getStyle());

            // 数据库获取允许的配图方式
            List<String> enabledMethods = null;
            if (article.getEnabledImageMethods() != null) {
                enabledMethods = GsonUtils.fromJson(article.getEnabledImageMethods(), new TypeToken<List<String>>() {});
            }
            state.setEnabledImageMethods(enabledMethods);

            //设置标题
            ArticleState.TitleResult title = new ArticleState.TitleResult();
            title.setMainTitle(article.getMainTitle());
            title.setSubTitle(article.getSubTitle());
            state.setTitle(title);

            //设置大纲
            List<ArticleState.OutlineSection> outlineSections = GsonUtils.fromJson(article.getOutline(), new TypeToken<List<ArticleState.OutlineSection>>() {
            });
            ArticleState.OutlineResult outlineResult = new ArticleState.OutlineResult();
            outlineResult.setSections(outlineSections);
            state.setOutline(outlineResult);

            if (orchestratorEnabled) {
                articleAgentOrchestrator.executePhase3_GenerateContent(state,message -> {
                    handleAgentMessage(taskId,message,state);
                });
            } else {
                //执行阶段3：生成正文 + 配图
                articleAgentService.executePhase3_GenerateContent(state,message -> {
                    handleAgentMessage(taskId,message,state);
                });
            }

            //保存完整文章到数据库
            articleService.saveArticleContent(taskId,state);

            //更新状态为已完成
            articleService.updateArticleStatus(taskId,ArticleStatusEnum.COMPLETED,null);

            //推送完成消息
            sendSseMessage(taskId,SseMessageTypeEnum.ALL_COMPLETE,Map.of("taskId",taskId));

            //完成SSE连接
            sseEmitterManager.complete(taskId);
            log.info("阶段3异步任务完成,taskId={}",taskId);
        }catch (Exception e) {
            log.info("阶段3异步任务失败,taskId = {}",taskId,e);
            articleService.updateArticleStatus(taskId,ArticleStatusEnum.FAILED,e.getMessage());
            sendSseMessage(taskId,SseMessageTypeEnum.ERROR,Map.of("message",e.getMessage()));
            sseEmitterManager.complete(taskId);
        }
    }



    /**
     * 异步执行文章生成
     * @param taskId
     * @param topic
     * @param style
     * @param enabledImageMethods
     */
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
    private void sendSseMessage(String taskId, SseMessageTypeEnum type, Map<String, Object> additionalData) {
        HashMap<String, Object> data = new HashMap<>();
        data.put("type",type.getValue());
        data.putAll(additionalData);
        sseEmitterManager.send(taskId,GsonUtils.toJson(data));
    }
}
