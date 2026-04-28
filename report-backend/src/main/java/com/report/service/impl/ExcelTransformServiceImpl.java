package com.report.service.impl;

import com.report.entity.ReportConfig;
import com.report.mapper.ReportConfigMapper;
import com.report.service.ExcelTransformService;
import com.report.service.TransformResult;
import com.report.util.FileNameDateExtractor;
import com.report.util.StandardExcelWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
public class ExcelTransformServiceImpl implements ExcelTransformService {

    @Autowired
    private ReportConfigMapper reportConfigMapper;

    @Override
    public TransformResult transform(String filePath, Long reportConfigId) {
        TransformResult result = new TransformResult();
        ReportConfig config = reportConfigMapper.selectById(reportConfigId);

        if (config == null) {
            result.setSuccess(false);
            result.setErrorMessage("Report config not found: " + reportConfigId);
            return result;
        }

        try {
            List<String> headers = new ArrayList<>();
            List<Map<String, Object>> rows = new ArrayList<>();

            try (FileInputStream fis = new FileInputStream(filePath);
                 Workbook workbook = new XSSFWorkbook(fis)) {

                Sheet sheet = workbook.getSheetAt(config.getSheetIndex() != null ? config.getSheetIndex() : 0);
                int startRow = config.getStartRow() != null ? config.getStartRow() - 1 : (config.getHeaderRow() != null ? config.getHeaderRow() : 0);
                int startCol = config.getStartCol() != null ? config.getStartCol() - 1 : 0;

                Row headerRow = sheet.getRow(startRow);
                if (headerRow == null) {
                    result.setSuccess(false);
                    result.setErrorMessage("Header row not found at row " + (startRow + 1));
                    return result;
                }

                for (int i = startCol; i < headerRow.getLastCellNum(); i++) {
                    Cell cell = headerRow.getCell(i);
                    String colName = getCellStringValue(cell);
                    if (colName == null || colName.trim().isEmpty()) {
                        colName = "col_" + (i - startCol + 1);
                    }
                    headers.add(colName.trim());
                }

                int dataStartRow = startRow + 1;
                for (int rowIdx = dataStartRow; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                    Row row = sheet.getRow(rowIdx);
                    if (row == null) continue;
                    Map<String, Object> rowData = new LinkedHashMap<>();
                    boolean hasData = false;
                    for (int colIdx = startCol; colIdx < startCol + headers.size(); colIdx++) {
                        Cell cell = row.getCell(colIdx);
                        Object value = getCellValue(cell);
                        rowData.put(headers.get(colIdx - startCol), value);
                        if (value != null && !value.toString().trim().isEmpty()) {
                            hasData = true;
                        }
                    }
                    if (hasData) {
                        rows.add(rowData);
                    }
                }
            }

            String outputDir = System.getProperty("java.io.tmpdir") + File.separator + "standard-excel";
            new File(outputDir).mkdirs();
            String sourceFileName = new File(filePath).getName();
            String outputFile = outputDir + File.separator + sourceFileName.replace(".xlsx", "_standard.xlsx").replace(".xls", "_standard.xlsx");

            StandardExcelWriter.write(outputFile, headers, rows);

            result.setSuccess(true);
            result.setStandardExcelPath(outputFile);
            result.setDbName("ods_layer");
            result.setTableName(config.getOdsTableName());
            result.setFieldMappingJson(buildFieldMappingJson(headers, config.getColumnMapping()));
            result.setPtDt(extractPtDt(sourceFileName));

            log.info("Excel transform success: {} rows, output={}", rows.size(), outputFile);

        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            log.error("Excel transform failed: {}", filePath, e);
        }

        return result;
    }

    private String extractPtDt(String fileName) {
        LocalDate date = FileNameDateExtractor.extractDate(fileName);
        if (date == null) {
            date = LocalDate.now();
            log.info("文件名没有日期，使用当前日期: {}", date);
        }
        if (date.getYear() < 2020 || date.getYear() > 2100) {
            date = LocalDate.now();
            log.info("提取日期年份异常，使用当前日期: {}", date);
        }
        return date.toString();
    }

    private String buildFieldMappingJson(List<String> excelHeaders, String columnMapping) {
        try {
            Map<Integer, String> indexToFieldMap = new HashMap<>();
            if (columnMapping != null && !columnMapping.isEmpty()) {
                try {
                    List<Map> jsonList = new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                        columnMapping, List.class);
                    for (Map item : jsonList) {
                        String excelCol = (String) item.get("excelColumn");
                        String fieldName = (String) item.get("fieldName");
                        if (excelCol != null && fieldName != null) {
                            int colIndex = excelColToIndex(excelCol);
                            indexToFieldMap.put(colIndex, fieldName);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse column mapping JSON: {}", columnMapping, e);
                }
            }

            Map<String, String> fieldMap = new LinkedHashMap<>();
            for (int i = 0; i < excelHeaders.size(); i++) {
                String fieldName = indexToFieldMap.get(i);
                if (fieldName == null) {
                    fieldName = "field_" + (i + 1);
                }
                fieldMap.put(fieldName, "string");
            }
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(fieldMap);
        } catch (Exception e) {
            log.warn("Failed to build field mapping json", e);
            return "{}";
        }
    }

    private int excelColToIndex(String colLetter) {
        int result = 0;
        for (int i = 0; i < colLetter.length(); i++) {
            result = result * 26 + (colLetter.charAt(i) - 'A' + 1);
        }
        return result - 1;
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return null;
        }
    }

    private Object getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue();
                }
                return cell.getNumericCellValue();
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}
