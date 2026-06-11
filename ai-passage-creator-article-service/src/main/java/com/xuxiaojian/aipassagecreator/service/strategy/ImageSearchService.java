package com.xuxiaojian.aipassagecreator.service.strategy;

import com.xuxiaojian.aipassagecreator.model.dto.image.ImageData;
import com.xuxiaojian.aipassagecreator.model.dto.image.ImageRequest;
import com.xuxiaojian.aipassagecreator.model.enums.ImageMethodEnum;

/**
 * ClassName: ImageSearchService
 * Package: com.xuxiaojian.aipassagecreator.service
 * Description:
 *  策略接口
 *  图片检索服务接口
 *  抽象图片检索逻辑，便于扩展多种图片来源(如Pexels、Unsplash、AI生图等)
 *
 *  扩展新图片服务
 * @Author 阿健
 * @Create 2026-05-27 21:18
 * @Version 1.0
 */
public interface ImageSearchService {

    /**
     * 根据请求获取图片（推荐使用此方法）
     * @param request 图片请求对象，包含keywords、prompt等参数
     * @return 图片URL，获取失败返回null
     */
    default String getImage(ImageRequest request) {
        //判断是使用提示词 还是使用关键字
        String param = request.getEffectiveParam(getMethod().isAiGenerated());
        //结果需要看具体的实现,是base64 还是url
        return searchImage(param);
    }

    /**
     * 获取图片数据（用于统一上传到COS）
     * 子类可重写此方法返回更高效的数据格式（如字节数据）
     * @param request
     * @return
     */
    default ImageData getImageData(ImageRequest request) {
        //通过getImage获取url，然后转换为ImageData
        String url = getImage(request);
        return ImageData.fromUrl(url);
    }

    /**
     * 根据关键词检索图片(具体实现AI、或者关键字检索)
     * @param keywords 搜索关键词
     * @return url | 失败返回null
     */
    String searchImage(String keywords);

    /**
     * 获取图片检索方式
     * @return 图片检索方式枚举
     */
    ImageMethodEnum getMethod();

    /**
     * 获取降级图片URL
     * @param position 位置序号（用于生成唯一的随机图片）
     * @return 降级图片URL
     */
    String getFallbackImage(int position);

    /**
     * 判断服务是否可用
     * 子类可重写此方法进行健康检查
     * @return 服务是否可用
     */
    default boolean isAvailable() {
        return true;
    }
}
