package com.ncwu.iotservice.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Excel导出工具类
 */
public class ExcelExportUtil {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 导出设备数据到Excel
     * 
     * @param deviceCode 设备编号
     * @param dataList 数据列表
     * @return ResponseEntity<byte[]> Excel文件下载响应
     */
    public static ResponseEntity<byte[]> exportDeviceDataToExcel(String deviceCode, List<Map<String, Object>> dataList) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            
            // 创建工作表
            Sheet sheet = workbook.createSheet(deviceCode + " 设备数据报表");
            
            // 创建标题样式
            CellStyle titleStyle = createTitleStyle(workbook);
            
            // 创建表头
            Row headerRow = sheet.createRow(0);
            String[] headers = {"设备编号", "设备类型", "采集时间", "数据内容", "入库时间"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(titleStyle);
            }
            
            // 填充数据
            int rowNum = 1;
            for (Map<String, Object> data : dataList) {
                Row row = sheet.createRow(rowNum++);
                
                row.createCell(0).setCellValue(getStringValue(data.get("deviceCode")));
                row.createCell(1).setCellValue(getStringValue(data.get("deviceType")));
                row.createCell(2).setCellValue(formatDateTime(data.get("collectTime")));
                row.createCell(3).setCellValue(getStringValue(data.get("dataPayload")));
                row.createCell(4).setCellValue(formatDateTime(data.get("createTime")));
            }
            
            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // 写入输出流
            workbook.write(outputStream);
            
            // 设置响应头
            String fileName = deviceCode + "_设备数据报表_" + 
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + 
                            ".xlsx";
            
            HttpHeaders headers1 = new HttpHeaders();
            headers1.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            // 使用RFC 2231标准编码文件名，兼容性更好
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
            headers1.add("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
            headers1.setContentLength(outputStream.size());
            
            return ResponseEntity.ok()
                    .headers(headers1)
                    .body(outputStream.toByteArray());
                    
        } catch (IOException e) {
            throw new RuntimeException("导出Excel失败", e);
        }
    }
    
    /**
     * 创建标题样式
     */
    private static CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        
        // 背景色
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        // 字体
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        
        // 边框
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        
        // 居中
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        
        return style;
    }
    
    /**
     * 格式化日期时间
     */
    private static String formatDateTime(Object dateTime) {
        if (dateTime == null) {
            return "";
        }
        if (dateTime instanceof LocalDateTime) {
            return ((LocalDateTime) dateTime).format(DATE_FORMATTER);
        }
        return dateTime.toString();
    }
    
    /**
     * 获取字符串值
     */
    private static String getStringValue(Object value) {
        return value == null ? "" : value.toString();
    }
}
