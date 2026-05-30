package com.xuxiaojian.aipassagecreator.service.strategy.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import com.xuxiaojian.aipassagecreator.config.MermaidConfig;
import com.xuxiaojian.aipassagecreator.constant.ArticleConstant;
import com.xuxiaojian.aipassagecreator.exception.BusinessException;
import com.xuxiaojian.aipassagecreator.exception.ErrorCode;
import com.xuxiaojian.aipassagecreator.model.dto.image.ImageData;
import com.xuxiaojian.aipassagecreator.model.dto.image.ImageRequest;
import com.xuxiaojian.aipassagecreator.model.enums.ImageMethodEnum;
import com.xuxiaojian.aipassagecreator.service.strategy.ImageSearchService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * ClassName: MermaidService
 * Package: com.xuxiaojian.aipassagecreator.service.strategy.impl
 * Description:
 *
 * @Author 阿健
 * @Create 2026-05-30 0:24
 * @Version 1.0
 */
@Service
@Slf4j
public class MermaidService implements ImageSearchService {
    @Resource
    private MermaidConfig mermaidConfig;

    @Override
    public String searchImage(String keywords) {
        //此方法弃用,使用getImageData()
        return null;
    }

    @Override
    public String getImage(ImageRequest request) {
        //此方法弃用,使用getImageData()
        return null;
    }

    @Override
    public ImageData getImageData(ImageRequest request) {
        //优先使用prompt(Mermail代码)，是否使用keywords
        String mermaidCode = request.getEffectiveParam(true);
        return generateDiagramData(mermaidCode);
    }

    /**
     * 生成 Mermaid 图表数据
     * @param mermaidCode MermaidCode 代码
     * @return 图片字节数据，生成失败返回null
     */
    private ImageData generateDiagramData(String mermaidCode) {
        if (StrUtil.isBlank(mermaidCode)) {
            log.warn("Mermaid代码为空");
            return null;
        }

        File tempInputFile = null;
        File tempOutputFile = null;
        try {
            //创建临时输入文件
            tempInputFile = FileUtil.createTempFile("mermaid_input_",".mmd",true);
            FileUtil.writeUtf8String(mermaidCode,tempInputFile);

            //创建临时输出文件
            String outputExtension = "." + mermaidConfig.getOutputFormat();
            FileUtil.createTempFile("mermaid_output_",outputExtension,true);

            //转换为图片
            converMermaidToImage(tempInputFile,tempOutputFile);

            //检查输出文件
            if (!tempOutputFile.exists() || tempOutputFile.length() == 0) {
                log.error("Mermaid CLI 执行失败,输出文件不存在或为空");
                return null;
            }

            //读取图片字节数据
            byte[] imageBytes = FileUtil.readBytes(tempOutputFile);
            String mimeType = getMimeType(mermaidConfig.getOutputFormat());

            log.info("Mermaid 图标生成成功,size = {} bytes",imageBytes.length);
            return ImageData.fromBytes(imageBytes,mimeType);
        }catch (Exception e) {
            log.error("Mermaid 图标生成异常",e);
            return null;
        } finally {
            if (tempInputFile != null) {
                FileUtil.del(tempInputFile);
            }

            if (tempOutputFile != null) {
                FileUtil.del(tempOutputFile);
            }
        }
    }

    /**
     * 根据输出格式获取MIME类型
     * @param outputFormat
     * @return
     */
    private String getMimeType(String outputFormat) {
        return switch (outputFormat.toLowerCase()) {
            case "png" -> "image/png";
            case "svg" -> "image/svg+xml";
            case "pdf" -> "application/pdf";
            default -> "image/png";
        };
    }

    private void converMermaidToImage(File inputFile, File outputFile) {
        try {
            String command = SystemUtil.getOsInfo().isWindows() ? "mmdc.cmd" : mermaidConfig.getCliCommand();

            String cmdLine = String.format("%s -i %s -o %s -b %s", command, inputFile.getAbsolutePath(), outputFile.getAbsolutePath(),
                    mermaidConfig.getBackgroundColor());

            //如果配置了宽度,添加宽度参数
            if (mermaidConfig.getWidth() != null && mermaidConfig.getWidth() > 0) {
                cmdLine += " -w " + mermaidConfig.getWidth();
            }

            log.info("执行 Mermaid CLI命令:{}",cmdLine);

            //执行命令（带超时）
            String result = RuntimeUtil.execForStr(cmdLine);

            log.debug("Mermaid CLI 执行结果:{}",result);

        }catch (Exception e) {
            log.info("执行Mermaid CLI失败",e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"Mermaid CLI执行失败:" + e.getMessage());
        }
    }

    @Override
    public ImageMethodEnum getMethod() {
        return ImageMethodEnum.MERMAID;
    }

    @Override
    public String getFallbackImage(int position) {
        return String.format(ArticleConstant.PICSUM_URL_TEMPLATE,position);
    }

    @Override
    public boolean isAvailable() {
        try {
            String command = SystemUtil.getOsInfo().isWindows() ? "mmdc.cmd" : mermaidConfig.getCliCommand();
            String checkCmd = command + "--version";
            String version = RuntimeUtil.execForStr(checkCmd);
            log.info("Mermaid CLI版本: {}",version);
            return version != null && !version.isEmpty();
        }catch (Exception e) {
            log.warn("Mermail CLI不可用:{}",e.getMessage());
            return false;
        }
    }
}
