package com.xuxiaojian.aipassagecreator.service;

import com.xuxiaojian.aipassagecreator.exception.BusinessException;
import com.xuxiaojian.aipassagecreator.exception.ErrorCode;
import com.xuxiaojian.aipassagecreator.model.vo.StatisticsVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * 统计导出服务。
 * 按模板第一个 sheet 的第二行列头匹配统计字段，并将数据写入第三行。
 */
@Slf4j
@Service
public class StatisticsExportService {

    private static final int HEADER_ROW_INDEX = 1;

    private static final int DATA_ROW_INDEX = 2;

    private static final Map<String, Function<StatisticsVO, Object>> HEADER_VALUE_MAPPING = new LinkedHashMap<>();

    static {
        HEADER_VALUE_MAPPING.put("今日创作数量", StatisticsVO::getTodayCount);
        HEADER_VALUE_MAPPING.put("本周创作数量", StatisticsVO::getWeekCount);
        HEADER_VALUE_MAPPING.put("本月创作数量", StatisticsVO::getMonthCount);
        HEADER_VALUE_MAPPING.put("总创作数量", StatisticsVO::getTotalCount);
        HEADER_VALUE_MAPPING.put("成功率", statisticsVO -> formatPercentage(statisticsVO.getSuccessRate()));
        HEADER_VALUE_MAPPING.put("平均耗时", statisticsVO -> formatDuration(statisticsVO.getAvgDurationMs()));
        HEADER_VALUE_MAPPING.put("活跃用户数（本周）", StatisticsVO::getActiveUserCount);
        HEADER_VALUE_MAPPING.put("本周活跃用户数", StatisticsVO::getActiveUserCount);
        HEADER_VALUE_MAPPING.put("总用户数", StatisticsVO::getTotalUserCount);
        HEADER_VALUE_MAPPING.put("VIP 用户数", StatisticsVO::getVipUserCount);
        HEADER_VALUE_MAPPING.put("VIP用户数", StatisticsVO::getVipUserCount);
        HEADER_VALUE_MAPPING.put("配额总使用量", StatisticsVO::getQuotaUsed);
    }

    /**
     * 使用 Excel 模板导出统计数据。
     *
     * @param templateInputStream 模板输入流，不能为空
     * @param statisticsVO 统计数据，不能为空
     * @return 导出的 Excel 字节数组
     */
    public byte[] export(InputStream templateInputStream, StatisticsVO statisticsVO) {
        if (templateInputStream == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "模板文件不能为空");
        }
        if (statisticsVO == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "统计数据不能为空");
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (InputStream inputStream = templateInputStream;
             XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "模板中不存在工作表");
            }

            Row headerRow = sheet.getRow(HEADER_ROW_INDEX);
            if (headerRow == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "模板第二行列头不存在");
            }

            Row dataRow = sheet.getRow(DATA_ROW_INDEX);
            if (dataRow == null) {
                dataRow = sheet.createRow(DATA_ROW_INDEX);
            }

            boolean matched = false;
            short lastCellNum = headerRow.getLastCellNum();
            for (int cellIndex = 0; cellIndex < lastCellNum; cellIndex++) {
                Cell headerCell = headerRow.getCell(cellIndex);
                if (headerCell == null) {
                    continue;
                }
                String headerName = headerCell.toString();
                if (headerName.trim().isEmpty()) {
                    continue;
                }
                Function<StatisticsVO, Object> valueGetter = HEADER_VALUE_MAPPING.get(headerName.trim());
                if (valueGetter == null) {
                    continue;
                }
                matched = true;
                Object value = valueGetter.apply(statisticsVO);
                if (value == null) {
                    continue;
                }
                Cell dataCell = dataRow.getCell(cellIndex);
                if (dataCell == null) {
                    dataCell = dataRow.createCell(cellIndex);
                }
                writeCellValue(dataCell, value);
            }

            if (!matched) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "模板中未匹配到可导出的统计列");
            }

            workbook.write(outputStream);
        } catch (BusinessException e) {
            log.info("统计模板导出失败,异常导出," + e.getMessage());
            throw e;
        } catch (Exception e) {
            log.info("统计模板导出失败,异常导出," + e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "统计模板导出失败");
        }

        // workbook 已关闭后再取字节，保证内容完整
        return outputStream.toByteArray();
    }

    private static String formatPercentage(Double value) {
        if (value == null) {
            return "0.00%";
        }
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP) + "%";
    }

    private static String formatDuration(Integer value) {
        if (value == null) {
            return "0ms";
        }
        return value + "ms";
    }

    private void writeCellValue(Cell cell, Object value) {
        if (value instanceof Number numberValue) {
            cell.setCellValue(numberValue.doubleValue());
            return;
        }
        cell.setCellValue(Objects.toString(value, ""));
    }
}