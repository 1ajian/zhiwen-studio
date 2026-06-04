package com.xuxiaojian.aipassagecreator.agent.parallel;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.xuxiaojian.aipassagecreator.agent.context.StreamHandlerContext;
import com.xuxiaojian.aipassagecreator.agent.tool.ImageGenerationTool;
import com.xuxiaojian.aipassagecreator.model.dto.article.ArticleState;
import com.xuxiaojian.aipassagecreator.model.enums.SseMessageTypeEnum;
import com.xuxiaojian.aipassagecreator.utils.GsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * ClassName: ParallelImageGenerator
 * Package: com.xuxiaojian.aipassagecreator.agent.parallel
 * Description:
 *
 * @Author 阿健
 * @Create 2026-06-04 19:37
 * @Version 1.0
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ParallelImageGenerator implements NodeAction {
    private final ImageGenerationTool imageGenerationTool;

    public static final String INPUT_IMAGE_REQUIREMENTS = "imageRequirements";
    public static final String OUTPUT_IMAGES = "images";

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        List<ArticleState.ImageRequirement> imageRequirements = state.value(INPUT_IMAGE_REQUIREMENTS)
                .map(v -> {
                    if (v instanceof List) {
                        List<?> list = (List<?>) v;
                        if (list.isEmpty()) {
                            return new ArrayList<ArticleState.ImageRequirement>();
                        }

                        if (list.get(0) instanceof ArticleState.ImageRequirement) {
                            return (List<ArticleState.ImageRequirement>) v;
                        }

                        return convertToImageRequirements(list);
                    }
                    return new ArrayList<ArticleState.ImageRequirement>();
                }).orElse(new ArrayList<>());

        // 从 ThreadLocal 获取流式处理器
        Consumer<String> streamHandler = StreamHandlerContext.get();

        log.info("ParallelImageGenerator 开始执行: 配图需求数量={}", imageRequirements.size());

        if (imageRequirements.isEmpty()) {
            log.info("没有配图需求，跳过图片生成");
            return Map.of(OUTPUT_IMAGES, new ArrayList<>());
        }
        Map<String, List<ArticleState.ImageRequirement>> groupBySource = imageRequirements.stream()
                .collect(Collectors.groupingBy(ArticleState.ImageRequirement::getImageSource));

        log.info("配图需求按类型分组: {}",
                groupBySource.entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue().size()
                        )));

        // 并行执行不同类型的图片生成
        List<ArticleState.ImageResult> allImages = executeParallel(groupBySource, streamHandler);
        // 按 position 排序
        allImages.sort((a, b) -> {
            Integer posA = a.getPosition() != null ? a.getPosition() : 0;
            Integer posB = b.getPosition() != null ? b.getPosition() : 0;
            return posA.compareTo(posB);
        });

        log.info("ParallelImageGenerator 执行完成: 成功生成 {} 张图片", allImages.size());

        return Map.of(OUTPUT_IMAGES, allImages);
    }

    private List<ArticleState.ImageResult> executeParallel(Map<String, List<ArticleState.ImageRequirement>> groupBySource, Consumer<String> streamHandler) {

        //使用线程安全的列表接收结果
        CopyOnWriteArrayList<ArticleState.ImageResult> allImages = new CopyOnWriteArrayList<>();
        List<CompletableFuture<Void>> futures = groupBySource.entrySet().stream().map(entry -> CompletableFuture.runAsync(
                () -> {
                    String imageSource = entry.getKey();
                    List<ArticleState.ImageRequirement> requirements = entry.getValue();
                    log.info("开始处理 {} 类型的图片，数量: {}", imageSource, requirements.size());
                    for (ArticleState.ImageRequirement req : requirements) {
                        try {
                            ImageGenerationTool.ImageGenerationResult result = imageGenerationTool.generateImageDirect(
                                    req.getImageSource(),
                                    req.getKeywords(),
                                    req.getPrompt(),
                                    req.getPosition(),
                                    req.getType(),
                                    req.getSectionTitle(),
                                    req.getPlaceholderId()
                            );

                            if (result.isSuccess()) {
                                ArticleState.ImageResult imageResult = convertToImageResult(result);
                                allImages.add(imageResult);

                                if (streamHandler != null) {
                                    String message = SseMessageTypeEnum.IMAGE_COMPLETE.getStreamingPrefix()
                                            + GsonUtils.toJson(imageResult);
                                    streamHandler.accept(message);
                                }

                                log.info("图片生成成功: imageSource={}, position={}",
                                        imageSource, req.getPosition());
                            } else {
                                log.warn("图片生成失败: imageSource={}, position={}, error={}",
                                        imageSource, req.getPosition(), result.getError());
                            }
                        } catch (Exception e) {
                            log.error("图片生成异常: imageSource={}, position={}",
                                    imageSource, req.getPosition(), e);
                        }
                    }
                    log.info("完成处理 {} 类型的图片", imageSource);
                }
        )).toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return new ArrayList<>(allImages);
    }

    private ArticleState.ImageResult convertToImageResult(ImageGenerationTool.ImageGenerationResult result) {
        ArticleState.ImageResult imageResult = new ArticleState.ImageResult();
        imageResult.setPosition(result.getPosition());
        imageResult.setUrl(result.getUrl());
        imageResult.setMethod(result.getMethod());
        imageResult.setKeywords(result.getKeywords());
        imageResult.setSectionTitle(result.getSectionTitle());
        imageResult.setDescription(result.getDescription());
        imageResult.setPlaceholder(result.getPlaceholderId());
        return imageResult;
    }

    /**
     * 转换列表为 ImageRequirement 列表
     */
    private List<ArticleState.ImageRequirement> convertToImageRequirements(List<?> list) {
        List<ArticleState.ImageRequirement> results = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof ArticleState.ImageRequirement) {
                results.add((ArticleState.ImageRequirement) item);
            } else if (item instanceof Map) {
                String json = GsonUtils.toJson(item);
                ArticleState.ImageRequirement req = GsonUtils.fromJson(json, ArticleState.ImageRequirement.class);
                results.add(req);
            }
        }
        return results;
    }
}
