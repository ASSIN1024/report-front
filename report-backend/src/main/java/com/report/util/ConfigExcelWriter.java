package com.report.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class ConfigExcelWriter {

    public static String write(String outputPath, List<Map<String, Object>> configRecords) throws Exception {
        File templateFile = new File("src/main/resources/informationTemplate.xlsx");

        Workbook workbook;
        Sheet sheet;

        if (templateFile.exists()) {
            try (FileInputStream fis = new FileInputStream(templateFile)) {
                workbook = new XSSFWorkbook(fis);
                sheet = workbook.getSheetAt(0);
            }
        } else {
            workbook = new XSSFWorkbook();
            sheet = workbook.createSheet("批量上传文件并生成数据表模板");
            createHeaders(sheet);
        }

        int startRow = sheet.getLastRowNum() + 1;
        if (startRow < 2) {
            startRow = 2;
        }

        for (int i = 0; i < configRecords.size(); i++) {
            Map<String, Object> record = configRecords.get(i);
            Row row = sheet.createRow(startRow + i);
            writeRecord(row, record, i + 1);
        }

        new File(outputPath).getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            workbook.write(fos);
        }
        workbook.close();
        return outputPath;
    }

    private static void createHeaders(Sheet sheet) {
        String[] headers = {
            "序号",
            "文件名",
            "目标表类型",
            "目标库名",
            "目标表名",
            "是否境外",
            "字段类型列表",
            "数据载入模式",
            "分区信息",
            "executor数量",
            "executor核数",
            "executor内存",
            "driver数量",
            "driver内存"
        };

        Row headerRow = sheet.createRow(0);
        headerRow.setHeight((short) 600);
        CellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        Font headerFont = sheet.getWorkbook().createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setWrapText(true);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private static void writeRecord(Row row, Map<String, Object> record, int seq) {
        CellStyle style = row.getSheet().getWorkbook().createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);

        setCell(row, 0, seq, style);
        setCell(row, 1, record.get("source_file"), style);
        setCell(row, 2, record.get("table_type"), style);
        setCell(row, 3, record.get("db_name"), style);
        setCell(row, 4, record.get("table_name"), style);
        setCell(row, 5, record.get("is_overseas"), style);
        setCell(row, 6, record.get("field_mapping"), style);
        setCell(row, 7, record.get("load_mode"), style);
        setCell(row, 8, record.get("partition_info"), style);
        setCell(row, 9, record.get("executor_num"), style);
        setCell(row, 10, record.get("executor_cores"), style);
        setCell(row, 11, record.get("executor_memory"), style);
        setCell(row, 12, record.get("driver_num"), style);
        setCell(row, 13, record.get("driver_memory"), style);
    }

    private static void setCell(Row row, int col, Object value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellStyle(style);
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else {
            cell.setCellValue(value.toString());
        }
    }
}