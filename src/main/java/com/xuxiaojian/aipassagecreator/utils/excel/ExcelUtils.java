package com.xuxiaojian.aipassagecreator.utils.excel;

import cn.hutool.core.util.StrUtil;
import com.xuxiaojian.aipassagecreator.exception.BusinessException;
import com.xuxiaojian.aipassagecreator.exception.ErrorCode;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.net.URLEncoder;

/**
 * ClassName: ExcelUtils
 * Package: com.xuxiaojian.aipassagecreator.utils
 * Description:
 *
 * @Author 阿健
 * @Create 2026-06-04 15:47
 * @Version 1.0
 */
public class ExcelUtils {

    public static void downloadTemplate(String excelLocalUrl,String fileName,HttpServletResponse response) {
        if (StrUtil.hasBlank(excelLocalUrl,fileName)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"模板地址或文件名不得为空");
        }

        ClassPathResource classPathResource = new ClassPathResource(excelLocalUrl);

        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String encodedFileName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename=" + encodedFileName);

            InputStream inputStream = classPathResource.getInputStream();
            ServletOutputStream outputStream = response.getOutputStream();
            StreamUtils.copy(inputStream,outputStream);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,String.format("下载模板{}出现异常",fileName));
        }

    }
}
