package com.xuxiaojian.aipassagecreator.controller;

import com.xuxiaojian.aipassagecreator.annotation.AuthCheck;
import com.xuxiaojian.aipassagecreator.common.BaseResponse;
import com.xuxiaojian.aipassagecreator.common.ResultUtils;
import com.xuxiaojian.aipassagecreator.constant.UserConstant;
import com.xuxiaojian.aipassagecreator.exception.BusinessException;
import com.xuxiaojian.aipassagecreator.exception.ErrorCode;
import com.xuxiaojian.aipassagecreator.model.vo.StatisticsVO;
import com.xuxiaojian.aipassagecreator.service.StatisticsExportService;
import com.xuxiaojian.aipassagecreator.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ClassName: StatisticsController
 * Package: com.xuxiaojian.aipassagecreator.controller
 * Description:
 *
 * @Author 阿健
 * @Create 2026-06-04 1:04
 * @Version 1.0
 */
@RestController
@RequestMapping("/statistics")
@Slf4j
@Tag(name = "StatisticsController", description = "统计分析接口")
public class StatisticsController {

    private static final String DEFAULT_TEMPLATE_PATH = "templates/statistics-export-template.xlsx";

    @Resource
    private StatisticsService statisticsService;

    @Resource
    private StatisticsExportService statisticsExportService;

    /**
     * 获取系统统计数据（仅管理员）
     */
    @GetMapping("/overview")
    @Operation(summary = "获取系统统计数据")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<StatisticsVO> getStatistics() {
        StatisticsVO statistics = statisticsService.getStatistics();
        return ResultUtils.success(statistics);
    }

    /**
     * 导出系统统计数据（仅管理员）。
     * 优先使用上传模板；未上传时回退到默认模板。
     *
     * @param templateFile 可选模板文件
     * @param response HTTP 响应对象
     */
    @PostMapping("/export")
    @Operation(summary = "导出系统统计数据")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public void exportStatistics(@RequestParam(value = "templateFile", required = false) MultipartFile templateFile,
                                 HttpServletResponse response) {
        StatisticsVO statistics = statisticsService.getStatistics();
        try (InputStream inputStream = getTemplateInputStream(templateFile)) {
            byte[] excelBytes = statisticsExportService.export(inputStream, statistics);
            writeExcelResponse(response, excelBytes);
        } catch (IOException e) {
            log.error("导出统计 Excel 失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "导出统计 Excel 失败");
        }
    }

    /**
     * 获取模板输入流。
     *
     * @param templateFile 可选上传模板
     * @return 模板输入流
     * @throws IOException 模板读取异常
     */
    private InputStream getTemplateInputStream(MultipartFile templateFile) throws IOException {
        if (templateFile != null && !templateFile.isEmpty()) {
            return templateFile.getInputStream();
        }

        ClassPathResource classPathResource = new ClassPathResource(DEFAULT_TEMPLATE_PATH);
        if (!classPathResource.exists()) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "默认统计模板不存在");
        }
        return classPathResource.getInputStream();
    }

    /**
     * 将 Excel 二进制直接写入响应流。
     *
     * @param response HTTP 响应对象
     * @param excelBytes Excel 二进制内容
     * @throws IOException 输出流写入异常
     */
    private void writeExcelResponse(HttpServletResponse response, byte[] excelBytes) throws IOException {
        String fileName = "AI爆款文章创作统计数据汇总-"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ".xlsx";

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String encodedFileName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename=" + encodedFileName);

        response.setContentLengthLong(excelBytes.length);
        response.getOutputStream().write(excelBytes);
        response.getOutputStream().flush();
    }
}
