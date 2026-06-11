package com.xuxiaojian.aipassagecreator.agent;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.xuxiaojian.aipassagecreator.agent.agents.*;
import com.xuxiaojian.aipassagecreator.agent.config.AgentConfig;
import com.xuxiaojian.aipassagecreator.agent.context.StreamHandlerContext;
import com.xuxiaojian.aipassagecreator.agent.parallel.ParallelImageGenerator;
import com.xuxiaojian.aipassagecreator.agent.tool.ImageGenerationTool;
import com.xuxiaojian.aipassagecreator.model.dto.article.ArticleState;
import com.xuxiaojian.aipassagecreator.model.enums.SseMessageTypeEnum;
import com.xuxiaojian.aipassagecreator.utils.GsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Consumer;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static com.xuxiaojian.aipassagecreator.agent.agents.ContentMergerAgent.INPUT_IMAGES;

/**
 * ClassName: ArticleAgentOrchestrator
 * Package: com.xuxiaojian.aipassagecreator.agent
 * Description:
 * 文章智能体编排器
 * 使用 Spring AI Alibaba 的 StateGraph 编排多个 Agent
 * @Author 阿健
 * @Create 2026-06-04 20:30
 * @Version 1.0
 */
@Service
@Slf4j
public class ArticleAgentOrchestrator {
    @Resource
    private AgentConfig agentConfig;
    @Resource
    private TitleGeneratorAgent titleGeneratorAgent;
    @Resource
    private OutlineGeneratorAgent outlineGeneratorAgent;
    @Resource
    private ContentGeneratorAgent contentGeneratorAgent;
    @Resource
    private ImageAnalyzerAgent imageAnalyzerAgent;
    @Resource
    private ParallelImageGenerator parallelImageGenerator;
    @Resource
    private ContentMergerAgent contentMergerAgent;

    // region 状态键常量

    private static final String KEY_TASK_ID = "taskId";
    private static final String KEY_TOPIC = "topic";
    private static final String KEY_STYLE = "style";
    private static final String KEY_USER_DESCRIPTION = "userDescription";
    private static final String KEY_MAIN_TITLE = "mainTitle";
    private static final String KEY_SUB_TITLE = "subTitle";
    private static final String KEY_TITLE_OPTIONS = "titleOptions";
    private static final String KEY_OUTLINE = "outline";
    private static final String KEY_CONTENT = "content";
    private static final String KEY_CONTENT_WITH_PLACEHOLDERS = "contentWithPlaceholders";
    private static final String KEY_IMAGE_REQUIREMENTS = "imageRequirements";
    private static final String KEY_IMAGES = "images";
    private static final String KEY_FULL_CONTENT = "fullContent";
    private static final String KEY_ENABLED_IMAGE_METHODS = "enabledImageMethods";

    // endregion

    /**
     * 阶段1：生成标题方案
     */
    public void executePhase1_GenerateTitles(ArticleState state, Consumer<String> streamHandler) {
        log.info("阶段1（多智能体编排）：开始生成标题方案, taskId={}", state.getTaskId());
        try {
            // 构建输入状态
            Map<String, Object> inputs = new HashMap<>();
            inputs.put(KEY_TASK_ID, state.getTaskId());
            inputs.put(KEY_TOPIC, state.getTopic());
            inputs.put(KEY_STYLE, state.getStyle());

            // 构建并执行图
            StateGraph graph = buildPhase1Graph();
            CompiledGraph compiledGraph = graph.compile();

            Optional<OverAllState> result = compiledGraph.invoke(inputs);

            if (result.isPresent()) {
                OverAllState finalState = result.get();

                List<ArticleState.TitleOption> titleOptions = finalState.value(KEY_TITLE_OPTIONS)
                        .map(v -> (List<ArticleState.TitleOption>) v)
                        .orElse(null);

                if (titleOptions != null) {
                    state.setTitleOptions(titleOptions);
                    streamHandler.accept(SseMessageTypeEnum.AGENT1_COMPLETE.getValue());
                    log.info("阶段1（多智能体编排）：标题方案生成完成, 数量={}", titleOptions.size());
                } else {
                    throw new RuntimeException("标题方案生成失败：结果为空");
                }
            }else {
                throw new RuntimeException("标题方案生成失败：执行结果为空");
            }
        }catch (Exception e) {
            log.error("阶段1（多智能体编排）：标题方案生成失败, taskId={}", state.getTaskId(), e);
            throw new RuntimeException("标题方案生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * 阶段2：生成大纲
     */
    public void executePhase2_GenerateOutline(ArticleState state, Consumer<String> streamHandler) {
        log.info("阶段2（多智能体编排）：开始生成大纲, taskId={}", state.getTaskId());

        // 设置流式处理器到 ThreadLocal
        StreamHandlerContext.set(streamHandler);

        try {
            Map<String, Object> inputs = new HashMap<>();
            inputs.put(KEY_TASK_ID, state.getTaskId());
            inputs.put(KEY_MAIN_TITLE, state.getTitle().getMainTitle());
            inputs.put(KEY_SUB_TITLE, state.getTitle().getSubTitle());
            inputs.put(KEY_USER_DESCRIPTION, state.getUserDescription());
            inputs.put(KEY_STYLE, state.getStyle());

            StateGraph graph = buildPhase2Graph();
            CompiledGraph compiledGraph = graph.compile();

            Optional<OverAllState> result = compiledGraph.invoke(inputs);

            if (result.isPresent()) {
                OverAllState finalState = result.get();

                ArticleState.OutlineResult outline = finalState.value(KEY_OUTLINE)
                        .map(v -> {
                            if (v instanceof ArticleState.OutlineResult) {
                                return (ArticleState.OutlineResult) v;
                            }
                            return GsonUtils.fromJson(GsonUtils.toJson(v), ArticleState.OutlineResult.class);
                        })
                        .orElse(null);

                if (outline != null) {
                    state.setOutline(outline);
                    streamHandler.accept(SseMessageTypeEnum.AGENT2_COMPLETE.getValue());
                    log.info("阶段2（多智能体编排）：大纲生成完成, 章节数={}", outline.getSections().size());
                } else {
                    throw new RuntimeException("大纲生成失败：结果为空");
                }
            } else {
                throw new RuntimeException("大纲生成失败：执行结果为空");
            }

        } catch (Exception e) {
            log.error("阶段2（多智能体编排）：大纲生成失败, taskId={}", state.getTaskId(), e);
            throw new RuntimeException("大纲生成失败: " + e.getMessage(), e);
        } finally {
            // 清理 ThreadLocal
            StreamHandlerContext.clear();
        }
    }

    /**
     * 阶段3：生成正文+配图
     */
    public void executePhase3_GenerateContent(ArticleState state, Consumer<String> streamHandler) {
        log.info("阶段3（多智能体编排）：开始生成正文+配图, taskId={}", state.getTaskId());

        StreamHandlerContext.set(streamHandler);

        try {
            HashMap<String, Object> inputs = new HashMap<>();
            inputs.put(KEY_TASK_ID, state.getTaskId());
            inputs.put(KEY_MAIN_TITLE, state.getTitle().getMainTitle());
            inputs.put(KEY_SUB_TITLE, state.getTitle().getSubTitle());
            inputs.put(KEY_OUTLINE, state.getOutline());
            inputs.put(KEY_STYLE, state.getStyle());
            inputs.put(KEY_ENABLED_IMAGE_METHODS, state.getEnabledImageMethods());

            StateGraph graph = buildPhase3Graph();
            CompiledGraph compiledGraph = graph.compile();

            Optional<OverAllState> result = compiledGraph.invoke(inputs);
            if (result.isPresent()) {
                OverAllState finalState = result.get();
                // 提取带占位符的正文
                String contentWithPlaceholders = finalState.value(KEY_CONTENT_WITH_PLACEHOLDERS)
                        .map(Object::toString)
                        .orElse(null);

                String content = finalState.value(KEY_CONTENT)
                        .map(Object::toString)
                        .orElse(null);

                @SuppressWarnings("unchecked")
                List<ArticleState.ImageRequirement> imageRequirements = finalState.value(KEY_IMAGE_REQUIREMENTS)
                        .map(v -> (List<ArticleState.ImageRequirement>) v)
                        .orElse(null);


                /*List<ArticleState.ImageResult> images = finalState.value(KEY_IMAGES)
                        .map(v -> (List<ArticleState.ImageResult>) v)
                        .orElse(null);*/

                List<ArticleState.ImageResult> images = finalState.value(INPUT_IMAGES).map(v -> {
                    if (v instanceof List) {
                        List<?> list = (List<?>) v;
                        if (list.isEmpty()) {
                            return new ArrayList<ArticleState.ImageResult>();
                        }

                        if (list.get(0) instanceof ArticleState.ImageResult) {
                            return (List<ArticleState.ImageResult>) v;
                        }

                        return convertToImageResults(list);
                    }
                    return new ArrayList<ArticleState.ImageResult>();
                }).orElse(new ArrayList<>());


                String fullContent = finalState.value(KEY_FULL_CONTENT)
                        .map(Object::toString)
                        .orElse(null);
                
                if (contentWithPlaceholders != null) {
                    state.setContent(contentWithPlaceholders);
                } else if (content != null) {
                    state.setContent(content);
                }
                streamHandler.accept(SseMessageTypeEnum.AGENT3_COMPLETE.getValue());

                if (imageRequirements != null) {
                    state.setImageRequirements(imageRequirements);
                    streamHandler.accept(SseMessageTypeEnum.AGENT4_COMPLETE.getValue());
                }

                if (images != null) {
                    state.setImages(images);
                    streamHandler.accept(SseMessageTypeEnum.AGENT5_COMPLETE.getValue());
                }

                if (fullContent != null) {
                    state.setFullContent(fullContent);
                    streamHandler.accept(SseMessageTypeEnum.MERGE_COMPLETE.getValue());
                }

                log.info("阶段3（多智能体编排）：正文+配图生成完成, 正文长度={}, 图片数={}",
                        contentWithPlaceholders != null ? contentWithPlaceholders.length() : 0,
                        images != null ? images.size() : 0);
            }else {
                throw new RuntimeException("正文+配图生成失败：执行结果为空");
            }
        }catch (Exception e) {
            log.error("阶段3（多智能体编排）：正文+配图生成失败, taskId={}", state.getTaskId(), e);
            throw new RuntimeException("正文+配图生成失败: " + e.getMessage(), e);
        } finally {
            StreamHandlerContext.clear();
        }
    }

    private StateGraph buildPhase3Graph() throws GraphStateException {
        KeyStrategyFactory keyStrategyFactory = createKeyStrategyFactory();
        return new StateGraph(keyStrategyFactory)
                //节点
                .addNode("content_generator",node_async(contentGeneratorAgent))
                .addNode("image_analyzer", node_async(imageAnalyzerAgent))
                .addNode("parallel_image_generator", node_async(parallelImageGenerator))
                .addNode("content_merger", node_async(contentMergerAgent))
                // 边定义：顺序执行
                .addEdge(START, "content_generator")
                .addEdge("content_generator", "image_analyzer")
                .addEdge("image_analyzer", "parallel_image_generator")
                .addEdge("parallel_image_generator", "content_merger")
                .addEdge("content_merger", END);

    }


    private StateGraph buildPhase2Graph() throws GraphStateException {
        KeyStrategyFactory keyStrategyFactory = createKeyStrategyFactory();

        return new StateGraph(keyStrategyFactory)
                .addNode("outline_generator", node_async(outlineGeneratorAgent))
                .addEdge(START, "outline_generator")
                .addEdge("outline_generator", END);
    }

    /**
     * 构建阶段1并行图
     * @return
     */
    private StateGraph buildPhase1Graph() throws GraphStateException {
        KeyStrategyFactory keyStrategyFactory = createKeyStrategyFactory();

        return new StateGraph(keyStrategyFactory)
                .addNode("title_generator", node_async(titleGeneratorAgent))
                .addEdge(START, "title_generator")
                .addEdge("title_generator", END);
    }

    private KeyStrategyFactory createKeyStrategyFactory() {
        return () -> {
            HashMap<String, KeyStrategy> strategies = new HashMap<>();
            strategies.put(KEY_TASK_ID, new ReplaceStrategy());
            strategies.put(KEY_TOPIC, new ReplaceStrategy());
            strategies.put(KEY_STYLE, new ReplaceStrategy());
            strategies.put(KEY_USER_DESCRIPTION, new ReplaceStrategy());
            strategies.put(KEY_MAIN_TITLE, new ReplaceStrategy());
            strategies.put(KEY_SUB_TITLE, new ReplaceStrategy());
            strategies.put(KEY_TITLE_OPTIONS, new ReplaceStrategy());
            strategies.put(KEY_OUTLINE, new ReplaceStrategy());
            strategies.put(KEY_CONTENT, new ReplaceStrategy());
            strategies.put(KEY_CONTENT_WITH_PLACEHOLDERS, new ReplaceStrategy());
            strategies.put(KEY_IMAGE_REQUIREMENTS, new ReplaceStrategy());
            strategies.put(KEY_IMAGES, new ReplaceStrategy());
            strategies.put(KEY_FULL_CONTENT, new ReplaceStrategy());
            strategies.put(KEY_ENABLED_IMAGE_METHODS, new ReplaceStrategy());
            return strategies;
        };
    }

    private List<ArticleState.ImageResult> convertToImageResults(List<?> list) {
        List<ArticleState.ImageResult> results = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof ArticleState.ImageResult) {
                results.add((ArticleState.ImageResult) item);
            } else if (item instanceof ImageGenerationTool.ImageGenerationResult) {
                ImageGenerationTool.ImageGenerationResult genResult =
                        (ImageGenerationTool.ImageGenerationResult) item;
                if (genResult.isSuccess()) {
                    ArticleState.ImageResult imageResult = new ArticleState.ImageResult();
                    imageResult.setPosition(genResult.getPosition());
                    imageResult.setUrl(genResult.getUrl());
                    imageResult.setMethod(genResult.getMethod());
                    imageResult.setKeywords(genResult.getKeywords());
                    imageResult.setSectionTitle(genResult.getSectionTitle());
                    imageResult.setDescription(genResult.getDescription());
                    imageResult.setPlaceholder(genResult.getPlaceholderId());
                    results.add(imageResult);
                }
            } else if (item instanceof Map) {
                String json = GsonUtils.toJson(item);
                ArticleState.ImageResult imageResult = GsonUtils.fromJson(json, ArticleState.ImageResult.class);
                if (imageResult.getUrl() != null) {
                    results.add(imageResult);
                }
            }
        }
        return results;
    }
}
