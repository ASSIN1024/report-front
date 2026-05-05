package com.report.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.report.entity.ReportConfig;
import com.report.entity.dto.CleanRule;
import com.report.entity.dto.ColumnMapping;
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
        return transform(filePath, reportConfigId, null);
    }

    @Override
    public TransformResult transform(String filePath, Long reportConfigId, String originalFileName) {
        TransformResult result = new TransformResult();
        ReportConfig config = reportConfigMapper.selectById(reportConfigId);

        if (config == null) {
            result.setSuccess(false);
            result.setErrorMessage("Report config not found: " + reportConfigId);
            return result;
        }

        List<ColumnMapping> columnMappings = parseColumnMapping(config.getColumnMapping());
        Map<String, ColumnMapping> headerToMapping = new HashMap<>();
        Map<Integer, ColumnMapping> indexToMapping = new HashMap<>();
        for (ColumnMapping mapping : columnMappings) {
            if (mapping.getExcelColumn() != null) {
                headerToMapping.put(mapping.getExcelColumn().trim(), mapping);
                if (mapping.getExcelColumn().matches("[A-Z]+")) {
                    indexToMapping.put(excelColToIndex(mapping.getExcelColumn()), mapping);
                }
            }
        }

        try {
            List<String> originalHeaders = new ArrayList<>();
            List<Map<String, Object>> rawRows = new ArrayList<>();

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
                    originalHeaders.add(colName.trim());
                }

                int dataStartRow = startRow + 1;
                for (int rowIdx = dataStartRow; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                    Row row = sheet.getRow(rowIdx);
                    if (row == null) continue;
                    Map<String, Object> rowData = new LinkedHashMap<>();
                    boolean hasData = false;
                    for (int colIdx = startCol; colIdx < startCol + originalHeaders.size(); colIdx++) {
                        Cell cell = row.getCell(colIdx);
                        Object value = getCellValue(cell);
                        String header = originalHeaders.get(colIdx - startCol);
                        ColumnMapping mapping = headerToMapping.get(header);
                        if (mapping == null) {
                            mapping = indexToMapping.get(colIdx - startCol);
                        }
                        if (mapping != null && mapping.getCleanRules() != null && !mapping.getCleanRules().isEmpty()) {
                            value = applyCleanRules(value, mapping.getCleanRules());
                        }
                        rowData.put(header, value);
                        if (value != null && !value.toString().trim().isEmpty()) {
                            hasData = true;
                        }
                    }
                    if (hasData) {
                        rawRows.add(rowData);
                    }
                }
            }

            Map<String, String> headerToFieldNameMap = new LinkedHashMap<>();
            Map<Integer, String> indexToFieldNameMap = new HashMap<>();
            Map<String, String> headerToFieldTypeMap = new HashMap<>();
            Map<Integer, String> indexToFieldTypeMap = new HashMap<>();
            for (ColumnMapping mapping : columnMappings) {
                if (mapping.getExcelColumn() != null && mapping.getFieldName() != null) {
                    headerToFieldNameMap.put(mapping.getExcelColumn().trim(), mapping.getFieldName());
                    headerToFieldTypeMap.put(mapping.getExcelColumn().trim(),
                        mapping.getFieldType() != null ? mapping.getFieldType() : "STRING");
                    if (mapping.getExcelColumn().matches("[A-Z]+")) {
                        int colIndex = excelColToIndex(mapping.getExcelColumn());
                        indexToFieldNameMap.put(colIndex, mapping.getFieldName());
                        indexToFieldTypeMap.put(colIndex,
                            mapping.getFieldType() != null ? mapping.getFieldType() : "STRING");
                    }
                }
            }

            List<String> mappedHeaders = new ArrayList<>();
            Map<String, String> sourceToFieldMap = new LinkedHashMap<>();
            for (int i = 0; i < originalHeaders.size(); i++) {
                String originalHeader = originalHeaders.get(i);
                String fieldName = headerToFieldNameMap.get(originalHeader);
                if (fieldName == null) {
                    fieldName = indexToFieldNameMap.get(i);
                }
                if (fieldName == null) {
                    fieldName = "field_" + (i + 1);
                }
                mappedHeaders.add(fieldName);
                sourceToFieldMap.put(originalHeader, fieldName);
            }

            String fileNameForDate = (originalFileName != null && !originalFileName.isEmpty())
                ? originalFileName : new File(filePath).getName();
            String ptDt = extractPtDt(fileNameForDate);

            List<String> standardHeaders = new ArrayList<>(mappedHeaders);
            standardHeaders.add("pt_dt");

            List<Map<String, Object>> standardRows = new ArrayList<>();
            List<Map<String, Object>> mappedRowsForOds = new ArrayList<>();
            for (Map<String, Object> rawRow : rawRows) {
                Map<String, Object> mappedRow = new LinkedHashMap<>();
                for (int i = 0; i < originalHeaders.size(); i++) {
                    String originalHeader = originalHeaders.get(i);
                    String fieldName = mappedHeaders.get(i);
                    mappedRow.put(fieldName, rawRow.get(originalHeader));
                }
                mappedRowsForOds.add(new LinkedHashMap<>(mappedRow));
                mappedRow.put("pt_dt", ptDt);
                standardRows.add(mappedRow);
            }

            String outputDir = System.getProperty("java.io.tmpdir") + File.separator + "standard-excel";
            new File(outputDir).mkdirs();
            String sourceFileName = new File(filePath).getName();
            long fileSize = new File(filePath).length();
            String outputFile = outputDir + File.separator + sourceFileName.replace(".xlsx", "_standard.xlsx").replace(".xls", "_standard.xlsx");

            StandardExcelWriter.write(outputFile, standardHeaders, standardRows);

            result.setSuccess(true);
            result.setSourceFile(sourceFileName);
            result.setStandardExcelPath(outputFile);
            result.setDbName(config.getTargetDbName() != null ? config.getTargetDbName() : "ods_layer");
            result.setTableName(config.getOdsTableName());
            result.setFieldMappingJson(buildFieldMappingJson(originalHeaders, config.getColumnMapping()));
            result.setSourceToFieldMapping(new ObjectMapper().writeValueAsString(sourceToFieldMap));
            result.setPtDt(ptDt);
            result.setLoadMode(config.getLoadMode() != null ? config.getLoadMode() : "partitioned-append");
            result.setFileSize(fileSize);
            result.setHeaders(new ArrayList<>(mappedHeaders));
            result.setRows(new ArrayList<>(mappedRowsForOds));

            log.info("Excel transform success: {} rows, mapped headers={}, output={}", standardRows.size(), mappedHeaders, outputFile);

        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            log.error("Excel transform failed: {}", filePath, e);
        }

        return result;
    }

    private Object applyCleanRules(Object value, List<CleanRule> cleanRules) {
        if (value == null) return value;
        String strValue = value.toString();
        for (CleanRule rule : cleanRules) {
            if (rule.getPattern() != null && rule.getReplace() != null) {
                strValue = strValue.replace(rule.getPattern(), rule.getReplace());
            }
        }
        return strValue;
    }

    private List<ColumnMapping> parseColumnMapping(String columnMappingJson) {
        List<ColumnMapping> mappings = new ArrayList<>();
        if (columnMappingJson == null || columnMappingJson.trim().isEmpty()) {
            return mappings;
        }
        try {
            List<Map> jsonList = new ObjectMapper().readValue(columnMappingJson, List.class);
            for (Map item : jsonList) {
                ColumnMapping mapping = new ColumnMapping();
                mapping.setExcelColumn((String) item.get("excelColumn"));
                mapping.setFieldName((String) item.get("fieldName"));
                mapping.setFieldType((String) item.get("fieldType"));
                mapping.setDateFormat((String) item.get("dateFormat"));

                List<Map> cleanRulesList = (List<Map>) item.get("cleanRules");
                if (cleanRulesList != null) {
                    List<CleanRule> rules = new ArrayList<>();
                    for (Map rule : cleanRulesList) {
                        CleanRule cr = new CleanRule();
                        cr.setPattern((String) rule.get("pattern"));
                        cr.setReplace((String) rule.get("replace"));
                        rules.add(cr);
                    }
                    mapping.setCleanRules(rules);
                }
                mappings.add(mapping);
            }
        } catch (Exception e) {
            log.warn("Failed to parse column mapping: {}", columnMappingJson, e);
        }
        return mappings;
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
            Map<String, String> headerToFieldMap = new HashMap<>();
            Map<Integer, String> indexToFieldMap = new HashMap<>();
            Map<String, String> headerToTypeMap = new HashMap<>();
            Map<Integer, String> indexToTypeMap = new HashMap<>();

            if (columnMapping != null && !columnMapping.isEmpty()) {
                try {
                    List<Map> jsonList = new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                        columnMapping, List.class);
                    for (Map item : jsonList) {
                        String excelCol = (String) item.get("excelColumn");
                        String fieldName = (String) item.get("fieldName");
                        String fieldType = (String) item.get("fieldType");
                        if (excelCol != null && fieldName != null) {
                            headerToFieldMap.put(excelCol.trim(), fieldName);
                            if (fieldType != null) {
                                headerToTypeMap.put(excelCol.trim(), fieldType);
                            }
                            int colIndex = excelColToIndex(excelCol);
                            indexToFieldMap.put(colIndex, fieldName);
                            if (fieldType != null) {
                                indexToTypeMap.put(colIndex, fieldType);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse column mapping JSON: {}", columnMapping, e);
                }
            }

            Map<String, Map<String, String>> fieldMap = new LinkedHashMap<>();
            for (int i = 0; i < excelHeaders.size(); i++) {
                String header = excelHeaders.get(i);
                String fieldName = headerToFieldMap.get(header);

                if (fieldName == null) {
                    fieldName = indexToFieldMap.get(i);
                }

                if (fieldName == null) {
                    fieldName = "field_" + (i + 1);
                }

                String fieldType = headerToTypeMap.get(header);
                if (fieldType == null) {
                    fieldType = indexToTypeMap.get(i);
                }
                if (fieldType == null) {
                    fieldType = "STRING";
                }

                Map<String, String> fieldInfo = new HashMap<>();
                fieldInfo.put("type", fieldType);
                fieldMap.put(fieldName, fieldInfo);
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
