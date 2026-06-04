package com.xuxiaojian.aipassagecreator.service.strategy.impl;

import com.google.genai.Client;
import com.google.genai.types.ClientOptions;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.ImageConfig;
import com.google.genai.types.Part;
import com.google.genai.types.ProxyOptions;
import com.google.genai.types.ProxyType;
import com.xuxiaojian.aipassagecreator.config.NanoBananaConfig;
import com.xuxiaojian.aipassagecreator.constant.ArticleConstant;
import com.xuxiaojian.aipassagecreator.model.dto.image.ImageData;
import com.xuxiaojian.aipassagecreator.model.dto.image.ImageRequest;
import com.xuxiaojian.aipassagecreator.model.enums.ImageMethodEnum;
import com.xuxiaojian.aipassagecreator.service.strategy.ImageSearchService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * ClassName: NanoBananaService
 * Package: com.xuxiaojian.aipassagecreator.service.strategy.impl
 * Description:
 *
 * @Author 阿健
 * @Create 2026-05-29 16:54
 * @Version 1.0
 */
@Service
@Slf4j
public class NanoBananaService implements ImageSearchService {

    @Resource
    private NanoBananaConfig nanoBananaConfig;

    @Override
    public String searchImage(String keywords) {
        return null;
    }

    @Override
    public ImageData getImageData(ImageRequest request) {
        String prompt = request.getEffectiveParam(getMethod().isAiGenerated());
        return generateImageData(prompt);
    }

    /**
     * 根据提示词生成图片数据
     * @param prompt 生图提示词
     * @return ImageData包含图片字节数据,生成失败返回null
     */
    private ImageData generateImageData(String prompt) {
        try {
            Client genaiClient = createGenaiClient();

            try {
                // 构建图片配置
                ImageConfig.Builder imageConfigBuilder = ImageConfig.builder()
                        .aspectRatio(nanoBananaConfig.getAspectRatio());

                //Gemini3 Pro Image支持更高分辨率
                String model = nanoBananaConfig.getModel();
                if (model != null && model.contains("gemini-3-pro")) {
                    imageConfigBuilder.imageSize(nanoBananaConfig.getImageSize());
                }

                //构建生成配置
                GenerateContentConfig config = GenerateContentConfig.builder()
                        .responseModalities("TEXT", "IMAGE")
                        .imageConfig(imageConfigBuilder.build())
                        .build();

                log.info("Nano Banana 开始生成图片,model={},prompt={}",model,prompt);

                GenerateContentResponse response = genaiClient.models.generateContent(
                        model != null ? model : "gemini-2.5-flash-image",
                        prompt,
                        config
                );

                // 从响应中提取图片数据
                if (response.parts() != null) {
                    for (Part part : response.parts()) {
                        if (part.inlineData().isPresent()) {
                            var blob = part.inlineData().get();
                            if (blob.data().isPresent()) {
                                byte[] imageBytes = blob.data().get();
                                String mimeType = blob.mimeType().orElse("image/png");
                                log.info("Nano Banana 图片生成完成,size = {} bytes,mimeType = {}",
                                        imageBytes.length,mimeType);

                                return ImageData.fromBytes(imageBytes,mimeType);
                            }
                        }
                    }
                }

                log.warn("Nano Banana 未生成图片，prompt={}",prompt);
                return null;
            }finally {
                genaiClient.close();
            }
        } catch (Exception e) {
            log.warn("Nano Banana 未生成图片，prompt={}",prompt,e);
            return null;
        }
    }

    @Override
    public ImageMethodEnum getMethod() {
        return ImageMethodEnum.NANO_BANANA;
    }

    @Override
    public String getFallbackImage(int position) {
        return String.format(ArticleConstant.PICSUM_URL_TEMPLATE,position);
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    /**
     * 创建 Google GenAI 客户端。
     * 启用专用代理时，仅当前 Nano Banana 客户端走代理，避免影响其他 HTTP 请求。
     *
     * @return Google GenAI 客户端
     */
    private Client createGenaiClient() {
        Client.Builder builder = Client.builder()
                .apiKey(nanoBananaConfig.getApiKey());

        if (!Boolean.TRUE.equals(nanoBananaConfig.getProxyEnabled())) {
            log.info("Nano Banana 未启用专用代理，将直接请求 Google GenAI 接口");
            return builder.build();
        }

        String proxyHost = nanoBananaConfig.getProxyHost();
        Integer proxyPort = nanoBananaConfig.getProxyPort();
        if (proxyHost == null || proxyHost.isBlank() || proxyPort == null) {
            throw new IllegalStateException("Nano Banana 已启用专用代理，但 proxyHost 或 proxyPort 未正确配置");
        }

        ProxyOptions proxyOptions = ProxyOptions.builder()
                .type(ProxyType.Known.HTTP)
                .host(proxyHost)
                .port(proxyPort)
                .build();

        ClientOptions clientOptions = ClientOptions.builder()
                .proxyOptions(proxyOptions)
                .build();

        log.info("Nano Banana 已启用专用代理: type=HTTP, host={}, port={}", proxyHost, proxyPort);
        return builder.clientOptions(clientOptions).build();
    }
}
