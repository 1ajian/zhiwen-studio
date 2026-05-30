package com.xuxiaojian.aipassagecreator.service.strategy;

import com.xuxiaojian.aipassagecreator.model.dto.image.ImageData;
import com.xuxiaojian.aipassagecreator.model.dto.image.ImageRequest;
import com.xuxiaojian.aipassagecreator.model.enums.ImageMethodEnum;
import com.xuxiaojian.aipassagecreator.service.CosService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * ClassName: ImageServiceStrategy
 * Package: com.xuxiaojian.aipassagecreator.service
 * Description:
 *  图片服务策略选择器
 *  根据图片来源类型选择对应的图片服务实现
 *
 *  设计说明:
 *      自动注册所有 ImageSearchService 实现
 *      根据 ImageMethodEnum的元数据自动选择正确的参数
 *      支持服务可用性检查和自动降级
 *      统一处理图片上传到COS
 * @Author 阿健
 * @Create 2026-05-29 14:32
 * @Version 1.0
 */
@Service
@Slf4j
public class ImageServiceStrategy {

    @Resource
    private List<ImageSearchService> imageSearchServices;

    @Resource
    private CosService cosService;

    private final Map<ImageMethodEnum,ImageSearchService> serviceMap = new EnumMap<>(ImageMethodEnum.class);

    @PostConstruct
    public void init() {
        for (ImageSearchService service : imageSearchServices) {
            ImageMethodEnum method = service.getMethod();
            serviceMap.put(method,service);
            log.info("注册图片服务:{}->{} (AI生图:{},降级:{})",
                    method.getValue(),
                    service.getClass().getSimpleName(),
                    method.isAiGenerated(),
                    method.isFallback());
        }
    }

    /**
     * 获取图片并上传到COS
     * 统一处理所有图片来源的上传逻辑
     * @param imageSource 图片来源
     * @param request 图片请求对象
     * @return 图片获取结果（包含COS URL）
     */
    public ImageResult getImageAndUpload(String imageSource, ImageRequest request) {
        ImageMethodEnum method = resolveMethod(imageSource);
        ImageSearchService service = getService(method);

        if (service == null || !service.isAvailable()) {
            log.warn("图片服务不可用:{},尝试降级",method);
            return handleFallbackWithUpload(request.getPosition());
        }

        try {
            //获取图片数据
            ImageData imageData = service.getImageData(request);

            if (imageData == null || !imageData.isValid()) {
                log.warn("图片数据获取失败,使用降级方案,method={}",method);
                return handleFallbackWithUpload(request.getPosition());
            }

            //上传到COS
            String folder = getFolderForMethod(method);
            String cosUrl = cosService.uploadImageData(imageData, folder);

            if (cosUrl != null && !cosUrl.isEmpty()) {
                log.info("图片获取并上传成功,method={},cosUrl={}",method,cosUrl);
                return new ImageResult(cosUrl,method);
            } else {
                log.warn("图片上传COS失败,使用降级方案,method={}",method);
                return handleFallbackWithUpload(request.getPosition());
            }
        }catch (Exception e) {
            log.error("获取图片并上传异常,method={}",method,e);
            return handleFallbackWithUpload(request.getPosition());
        }
    }

    /**
     * 根据图片方法枚举获取对应服务
     * @param method
     * @return
     */
    private ImageSearchService getService(ImageMethodEnum method) {
        return serviceMap.get(method);
    }

    /**
     * 获取所有已注册的图片服务类型
     * @return
     */
    public List<ImageMethodEnum> getRegisteredMethods() {
        return List.copyOf(serviceMap.keySet());
    }

    /**
     * 根据图片方式获取COS文件夹
     * @param method
     * @return
     */
    private String getFolderForMethod(ImageMethodEnum method) {
        return switch (method) {
            case PEXELS -> "pexels";
            case NANO_BANANA -> "nano-banana";
            case MERMAID -> "mermaid";
            case ICONIFY -> "iconify";
            case EMOJI_PACK -> "emoji-pack";
            case SVG_DIAGRAM -> "svg-diagram";
            case PICSUM -> "picsum";
        };
    }

    /**
     * 处理降级逻辑（含上传）
     * @param position
     * @return
     */
    private ImageResult handleFallbackWithUpload(Integer position) {
        int pos = position != null ? position : 1;
        String fallbackUrl = getFallbackImage(pos);

        //将降级图片也上传到COS
        ImageData fallbackData = ImageData.fromUrl(fallbackUrl);
        String cosUrl = cosService.uploadImageData(fallbackData, "fallback");

        //如果上传失败,直接使用原始URL
        String finalUrl = (cosUrl != null && !cosUrl.isEmpty()) ? cosUrl : fallbackUrl;
        return new ImageResult(finalUrl,ImageMethodEnum.getFallbackMethod());
    }

    /**
     * 获取降级图片
     * @param position
     * @return
     */
    private String getFallbackImage(int position) {
        ImageSearchService defaultService = serviceMap.get(ImageMethodEnum.getDefaultSearchMethod());
        if (defaultService != null) {
            return defaultService.getFallbackImage(position);
        }

        return String.format("https://picsum.photos/800/600?random=%d",position);
    }

    /**
     * 解析图片来源，处理未知值
     * @param imageSource
     * @return
     */
    private ImageMethodEnum resolveMethod(String imageSource) {
        ImageMethodEnum method = ImageMethodEnum.getByValue(imageSource);
        if (method == null) {
            log.warn("未知的图片来源:{},默认使用 {}",imageSource,ImageMethodEnum.getDefaultAiMethod());
            return ImageMethodEnum.getDefaultAiMethod();
        }

        return method;
    }

    /**
     * 图片获取结果
     */
    public static class ImageResult {
        private final String url;
        private final ImageMethodEnum method;

        public ImageResult(String url, ImageMethodEnum method) {
            this.url = url;
            this.method = method;
        }

        public String getUrl() {
            return url;
        }

        public ImageMethodEnum getMethod() {
            return method;
        }

        public boolean isSuccess() {
            return url != null && !url.isEmpty();
        }
    }
}
