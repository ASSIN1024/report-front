package com.report.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;

public class StandardExcelWriter {

    private static final Logger log = LoggerFactory.getLogger(StandardExcelWriter.class);

    public static String write(String outputPath, List<String> headers, List<Map<String, Object>> rows) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Data");

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers.get(i));
            cell.setCellStyle(headerStyle);
        }

        for (int rowIdx = 0; rowIdx < rows.size(); rowIdx++) {
            Row row = sheet.createRow(rowIdx + 1);
            Map<String, Object> data = rows.get(rowIdx);
            for (int colIdx = 0; colIdx < headers.size(); colIdx++) {
                Cell cell = row.createCell(colIdx);
                Object value = data.get(headers.get(colIdx));
                setCellValue(cell, value);
            }
        }

        for (int i = 0; i < headers.size(); i++) {
            sheet.autoSizeColumn(i);
        }

        new File(outputPath).getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            workbook.write(fos);
        }
        workbook.close();
        log.info("Standard Excel written: {}", outputPath);
        return outputPath;
    }

    private static void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setCellValue("");
            return;
        }
        if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else {
            cell.setCellValue(value.toString());
        }
    }
}
