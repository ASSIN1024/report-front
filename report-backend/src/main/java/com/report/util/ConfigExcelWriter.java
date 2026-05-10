package com.report.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.*;

public class ConfigExcelWriter {

    private static final Logger log = LoggerFactory.getLogger(ConfigExcelWriter.class);

    private static final short DATA_ROW_STYLE_INDEX = 5;

    public static String write(String outputPath, List<Map<String, Object>> configRecords) throws Exception {
        Workbook workbook;
        Sheet sheet;
        CellStyle[] dataRowStyles = null;

        InputStream templateStream = ConfigExcelWriter.class.getClassLoader()
            .getResourceAsStream("informationTemplate.xlsx");

        if (templateStream != null) {
            try (InputStream is = templateStream) {
                workbook = new XSSFWorkbook(is);
                sheet = workbook.getSheetAt(0);
                dataRowStyles = extractDataRowStyles(sheet, workbook);
                log.info("Loaded informationTemplate.xlsx from classpath");
            }
        } else {
            File templateFile = new File("src/main/resources/informationTemplate.xlsx");
            if (templateFile.exists()) {
                workbook = new XSSFWorkbook(new java.io.FileInputStream(templateFile));
                sheet = workbook.getSheetAt(0);
                dataRowStyles = extractDataRowStyles(sheet, workbook);
                log.info("Loaded informationTemplate.xlsx from file system");
            } else {
                workbook = new XSSFWorkbook();
                sheet = workbook.createSheet("批量上传文件并生成数据表模板");
                createHeaders(sheet);
                log.info("Created new workbook with headers (template not found)");
            }
        }

        int startRow = findDataStartRow(sheet);

        for (int i = sheet.getLastRowNum(); i >= startRow; i--) {
            Row row = sheet.getRow(i);
            if (row != null) {
                sheet.removeRow(row);
            }
        }

        for (int i = 0; i < configRecords.size(); i++) {
            Map<String, Object> record = configRecords.get(i);
            Row row = sheet.createRow(startRow + i);
            writeRecord(row, record, i + 1, dataRowStyles);
        }

        File outputFile = new File(outputPath);
        File parentDir = outputFile.getParentFile();
        if (parentDir != null) {
            parentDir.mkdirs();
        }
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            workbook.write(fos);
        }
        workbook.close();
        log.info("informationTemplate.xlsx written: {} ({} records)", outputPath, configRecords.size());
        return outputPath;
    }

    private static CellStyle[] extractDataRowStyles(Sheet sheet, Workbook workbook) {
        CellStyle[] styles = new CellStyle[9];
        Row templateRow = sheet.getRow(3);
        if (templateRow == null) {
            templateRow = sheet.getRow(2);
        }
        if (templateRow == null) {
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row r = sheet.getRow(i);
                if (r != null && r.getRowNum() > 1) {
                    templateRow = r;
                    break;
                }
            }
        }

        if (templateRow != null) {
            for (int i = 0; i < 9; i++) {
                Cell cell = templateRow.getCell(i);
                if (cell != null && cell.getCellStyle() != null) {
                    styles[i] = cell.getCellStyle();
                } else {
                    styles[i] = workbook.getCellStyleAt(DATA_ROW_STYLE_INDEX);
                }
            }
            log.info("Extracted data row styles from template row {}", templateRow.getRowNum());
        } else {
            for (int i = 0; i < 9; i++) {
                styles[i] = workbook.getCellStyleAt(DATA_ROW_STYLE_INDEX);
            }
            log.info("Using default style index {}", DATA_ROW_STYLE_INDEX);
        }
        return styles;
    }

    private static int findDataStartRow(Sheet sheet) {
        int lastRow = sheet.getLastRowNum();
        if (lastRow < 0) {
            return 1;
        }
        for (int i = 0; i <= lastRow; i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            boolean hasData = false;
            for (int c = 0; c < row.getLastCellNum(); c++) {
                Cell cell = row.getCell(c);
                if (cell != null && cell.getCellType() != CellType.BLANK) {
                    String val = getCellStringValue(cell);
                    if (val != null && !val.trim().isEmpty()) {
                        hasData = true;
                        break;
                    }
                }
            }
            if (!hasData) {
                return i;
            }
        }
        return lastRow + 1;
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
            "分区信息"
        };

        Row headerRow = sheet.createRow(1);
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

    private static void writeRecord(Row row, Map<String, Object> record, int seq, CellStyle[] styles) {
        if (styles != null) {
            for (int i = 0; i < 9; i++) {
                Cell cell = row.createCell(i);
                if (styles[i] != null) {
                    CellStyle newStyle = row.getSheet().getWorkbook().createCellStyle();
                    newStyle.cloneStyleFrom(styles[i]);
                    newStyle.setAlignment(HorizontalAlignment.CENTER);
                    newStyle.setVerticalAlignment(VerticalAlignment.CENTER);
                    cell.setCellStyle(newStyle);
                }
            }
        }

        setCellValue(row, 0, seq);
        setCellValue(row, 1, record.get("standard_file"));
        setCellValue(row, 2, record.get("table_type"));
        setCellValue(row, 3, record.get("db_name"));
        setCellValue(row, 4, record.get("table_name"));
        setCellValue(row, 5, record.get("is_overseas"));
        setCellValue(row, 6, record.get("field_type_json"));
        setCellValue(row, 7, record.get("load_mode"));
        setCellValue(row, 8, record.get("partition_info"));
    }

    private static void setCellValue(Row row, int col, Object value) {
        Cell cell = row.getCell(col);
        if (cell == null) {
            cell = row.createCell(col);
        }
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

    private static String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                double d = cell.getNumericCellValue();
                if (d == Math.floor(d) && !Double.isInfinite(d)) {
                    return String.valueOf((long) d);
                }
                return String.valueOf(d);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return "";
        }
    }
}
