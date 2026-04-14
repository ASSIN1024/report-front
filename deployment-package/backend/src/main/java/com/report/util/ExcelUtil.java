package com.report.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.report.entity.ReportConfig;
import com.report.entity.dto.CleanRule;
import com.report.entity.dto.ColumnMapping;
import com.report.entity.dto.FieldMapping;
import com.report.entity.dto.Validators;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
public class ExcelUtil {

    private ExcelUtil() {}

    public static List<Map<String, Object>> parseExcel(byte[] data, int sheetIndex, int headerRow, int dataStartRow, List<ColumnMapping> columnMappings) throws IOException {
        List<Map<String, Object>> result = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(data))) {
            Sheet sheet = workbook.getSheetAt(sheetIndex);
            Map<Integer, String> headerMap = parseHeader(sheet, headerRow, columnMappings);
            
            int lastRowNum = sheet.getLastRowNum();
            for (int i = dataStartRow; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                Map<String, Object> rowData = parseRow(row, headerMap, columnMappings);
                if (!rowData.isEmpty()) {
                    rowData.put("pt_dt", new SimpleDateFormat("yyyyMMdd").format(new Date()));
                    result.add(rowData);
                }
            }
        }
        return result;
    }

    private static Map<Integer, String> parseHeader(Sheet sheet, int headerRow, List<ColumnMapping> columnMappings) {
        Map<Integer, String> headerMap = new HashMap<>();
        Row row = sheet.getRow(headerRow);
        if (row == null) {
            return headerMap;
        }
        
        Map<String, Integer> columnIndexMap = new HashMap<>();
        for (ColumnMapping mapping : columnMappings) {
            String col = mapping.getExcelColumn().toUpperCase();
            int index = col.charAt(0) - 'A';
            columnIndexMap.put(mapping.getFieldName(), index);
            headerMap.put(index, mapping.getFieldName());
        }
        
        return headerMap;
    }

    private static Map<String, Object> parseRow(Row row, Map<Integer, String> headerMap, List<ColumnMapping> columnMappings) {
        Map<String, Object> rowData = new HashMap<>();
        Map<String, ColumnMapping> mappingMap = new HashMap<>();
        for (ColumnMapping mapping : columnMappings) {
            mappingMap.put(mapping.getFieldName(), mapping);
        }
        
        boolean hasData = false;
        for (Map.Entry<Integer, String> entry : headerMap.entrySet()) {
            int colIndex = entry.getKey();
            String fieldName = entry.getValue();
            Cell cell = row.getCell(colIndex);
            ColumnMapping mapping = mappingMap.get(fieldName);
            
            Object value = getCellValue(cell, mapping);
            if (value != null) {
                hasData = true;
            }
            rowData.put(fieldName, value);
        }
        
        return hasData ? rowData : new HashMap<>();
    }

    private static Object getCellValue(Cell cell, ColumnMapping mapping) {
        if (cell == null) {
            return null;
        }
        
        CellType cellType = cell.getCellType();
        String fieldType = mapping != null ? mapping.getFieldType() : "STRING";
        
        try {
            switch (cellType) {
                case STRING:
                    String strValue = cell.getStringCellValue();
                    if ("INTEGER".equals(fieldType)) {
                        return Integer.parseInt(strValue);
                    } else if ("DECIMAL".equals(fieldType)) {
                        return new BigDecimal(strValue);
                    } else if ("DATE".equals(fieldType) || "DATETIME".equals(fieldType)) {
                        String dateFormat = mapping.getDateFormat();
                        if (dateFormat == null) {
                            dateFormat = "DATE".equals(fieldType) ? "yyyy-MM-dd" : "yyyy-MM-dd HH:mm:ss";
                        }
                        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
                        return sdf.parse(strValue);
                    }
                    return strValue;
                    
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        Date date = cell.getDateCellValue();
                        return date;
                    }
                    double numValue = cell.getNumericCellValue();
                    if ("INTEGER".equals(fieldType)) {
                        return (int) numValue;
                    } else if ("DECIMAL".equals(fieldType)) {
                        int scale = mapping != null && mapping.getScale() != null ? mapping.getScale() : 2;
                        return BigDecimal.valueOf(numValue).setScale(scale, BigDecimal.ROUND_HALF_UP);
                    }
                    return numValue;
                    
                case BOOLEAN:
                    return cell.getBooleanCellValue();
                    
                case FORMULA:
                    try {
                        Workbook wb = cell.getSheet().getWorkbook();
                        FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
                        CellValue evaluatedValue = evaluator.evaluate(cell);
                        return getCellValue(evaluatedValue, mapping);
                    } catch (Exception e) {
                        log.warn("公式计算失败: {}", e.getMessage());
                        return null;
                    }
                    
                default:
                    return null;
            }
        } catch (Exception e) {
            log.warn("解析单元格值失败: {}", e.getMessage());
            return null;
        }
    }

    private static Object getCellValue(CellValue cellValue, ColumnMapping mapping) {
        if (cellValue == null) {
            return null;
        }
        
        switch (cellValue.getCellType()) {
            case STRING:
                return cellValue.getStringValue();
            case NUMERIC:
                return cellValue.getNumberValue();
            case BOOLEAN:
                return cellValue.getBooleanValue();
            default:
                return null;
        }
    }

    public static List<String> getSheetNames(byte[] data) throws IOException {
        List<String> names = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(data))) {
            int numberOfSheets = workbook.getNumberOfSheets();
            for (int i = 0; i < numberOfSheets; i++) {
                names.add(workbook.getSheetName(i));
            }
        }
        return names;
    }

    public static List<List<String>> getHeaderRow(byte[] data, int sheetIndex, int headerRow) throws IOException {
        List<List<String>> headers = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(data))) {
            Sheet sheet = workbook.getSheetAt(sheetIndex);
            Row row = sheet.getRow(headerRow);
            if (row != null) {
                int lastCellNum = row.getLastCellNum();
                for (int i = 0; i < lastCellNum; i++) {
                    Cell cell = row.getCell(i);
                    List<String> cellInfo = new ArrayList<>();
                    cellInfo.add(getColumnName(i));
                    cellInfo.add(cell != null ? cell.toString() : "");
                    headers.add(cellInfo);
                }
            }
        }
        return headers;
    }

    private static String getColumnName(int index) {
        StringBuilder sb = new StringBuilder();
        while (index >= 0) {
            sb.insert(0, (char) ('A' + index % 26));
            index = index / 26 - 1;
        }
        return sb.toString();
    }

    public static List<Map<String, Object>> readExcel(File file, ReportConfig reportConfig, List<FieldMapping> columnMappings) throws IOException {
        List<Map<String, Object>> result = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(fis)) {

            int sheetIndex = reportConfig.getSheetIndex() != null ? reportConfig.getSheetIndex() : 0;
            int headerRow = reportConfig.getHeaderRow() != null ? reportConfig.getHeaderRow() : 0;
            int dataStartRow = reportConfig.getDataStartRow() != null ? reportConfig.getDataStartRow() : 1;
            int skipColumns = reportConfig.getSkipColumns() != null ? reportConfig.getSkipColumns() : 0;

            Sheet sheet = workbook.getSheetAt(sheetIndex);
            Map<Integer, FieldMapping> columnIndexMap = buildColumnIndexMap(sheet, headerRow, columnMappings, skipColumns);
            
            for (int i = dataStartRow; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                Map<String, Object> rowData = parseRowData(row, columnIndexMap);
                if (!rowData.isEmpty()) {
                    result.add(rowData);
                }
            }
        }
        return result;
    }

    private static Map<Integer, FieldMapping> buildColumnIndexMap(Sheet sheet, int headerRow, List<FieldMapping> columnMappings, int skipColumns) {
        Map<Integer, FieldMapping> map = new HashMap<>();
        Row row = sheet.getRow(headerRow);
        if (row != null) {
            for (Cell cell : row) {
                if (cell.getColumnIndex() < skipColumns) {
                    continue;
                }
                String columnName = getCellValueAsString(cell);
                if (StrUtil.isNotBlank(columnName)) {
                    for (FieldMapping mapping : columnMappings) {
                        if (columnName.trim().equalsIgnoreCase(mapping.getExcelColumn() != null ? mapping.getExcelColumn().trim() : "")) {
                            map.put(cell.getColumnIndex(), mapping);
                            break;
                        }
                    }
                }
            }
        }

        if (map.isEmpty()) {
            for (int i = 0; i < columnMappings.size(); i++) {
                FieldMapping mapping = columnMappings.get(i);
                String col = mapping.getExcelColumn();
                if (StrUtil.isNotBlank(col)) {
                    int index = col.toUpperCase().charAt(0) - 'A';
                    index += skipColumns;
                    map.put(index, mapping);
                }
            }
        }

        return map;
    }

    private static Map<String, Object> parseRowData(Row row, Map<Integer, FieldMapping> columnIndexMap) {
        Map<String, Object> rowData = new HashMap<>();
        boolean hasData = false;
        
        for (Map.Entry<Integer, FieldMapping> entry : columnIndexMap.entrySet()) {
            int colIndex = entry.getKey();
            FieldMapping mapping = entry.getValue();
            
            Cell cell = row.getCell(colIndex);
            Object value = getCellValueByMapping(cell, mapping);
            
            if (value != null) {
                hasData = true;
            }
            rowData.put(mapping.getFieldName(), value);
        }
        
        return hasData ? rowData : new HashMap<>();
    }

    private static Object getCellValueByMapping(Cell cell, FieldMapping mapping) {
        if (cell == null) {
            return null;
        }
        
        String fieldType = mapping.getFieldType() != null ? mapping.getFieldType() : "STRING";
        
        try {
            switch (cell.getCellType()) {
                case STRING:
                    String strValue = cell.getStringCellValue();
                    return convertStringValue(strValue, fieldType, mapping.getDateFormat());
                    
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        java.util.Date date = cell.getDateCellValue();
                        String dateFormat = mapping.getDateFormat();
                        if (StrUtil.isBlank(dateFormat)) {
                            dateFormat = "yyyy-MM-dd";
                        }
                        return new SimpleDateFormat(dateFormat).format(date);
                    }
                    double numValue = cell.getNumericCellValue();
                    return convertNumericValue(numValue, fieldType, mapping.getScale());
                    
                case BOOLEAN:
                    return cell.getBooleanCellValue();
                    
                case FORMULA:
                    try {
                        Workbook wb = cell.getSheet().getWorkbook();
                        FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
                        CellValue evaluatedValue = evaluator.evaluate(cell);
                        return convertCellValue(evaluatedValue, fieldType, mapping, cell);
                    } catch (Exception e) {
                        log.warn("公式计算失败: {}", e.getMessage());
                        return null;
                    }
                    
                default:
                    return null;
            }
        } catch (Exception e) {
            log.warn("解析单元格值失败: {}", e.getMessage());
            return null;
        }
    }

    private static Object convertStringValue(String strValue, String fieldType, String dateFormat) {
        if (StrUtil.isBlank(strValue)) {
            return null;
        }
        strValue = strValue.trim();
        
        try {
            switch (fieldType.toUpperCase()) {
                case "INTEGER":
                    return Long.parseLong(strValue.replaceAll("[,，]", ""));
                case "DECIMAL":
                    return new BigDecimal(strValue.replaceAll("[,，]", ""));
                case "DATE":
                case "DATETIME":
                    String pattern = StrUtil.isNotBlank(dateFormat) ? dateFormat : "yyyy-MM-dd";
                    java.util.Date dateValue = new SimpleDateFormat(pattern).parse(strValue);
                    return new SimpleDateFormat(pattern).format(dateValue);
                default:
                    return strValue;
            }
        } catch (Exception e) {
            log.warn("字符串转换失败: value={}, type={}", strValue, fieldType);
            return strValue;
        }
    }

    private static Object convertNumericValue(double numValue, String fieldType, Integer scale) {
        switch (fieldType.toUpperCase()) {
            case "INTEGER":
                return (long) numValue;
            case "DECIMAL":
                int s = scale != null ? scale : 2;
                return BigDecimal.valueOf(numValue).setScale(s, BigDecimal.ROUND_HALF_UP);
            default:
                return numValue;
        }
    }

    private static Object convertCellValue(CellValue cellValue, String fieldType, FieldMapping mapping, Cell originalCell) {
        if (cellValue == null) {
            return null;
        }
        
        switch (cellValue.getCellType()) {
            case STRING:
                return convertStringValue(cellValue.getStringValue(), fieldType, mapping.getDateFormat());
            case NUMERIC:
                if (originalCell != null && DateUtil.isCellDateFormatted(originalCell)) {
                    java.util.Date date = originalCell.getDateCellValue();
                    String dateFormat = mapping.getDateFormat();
                    if (StrUtil.isBlank(dateFormat)) {
                        dateFormat = "yyyy-MM-dd";
                    }
                    return new SimpleDateFormat(dateFormat).format(date);
                }
                return convertNumericValue(cellValue.getNumberValue(), fieldType, mapping.getScale());
            case BOOLEAN:
                return cellValue.getBooleanValue();
            default:
                return null;
        }
    }

    private static String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return new SimpleDateFormat("yyyy-MM-dd").format(cell.getDateCellValue());
                }
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return null;
        }
    }

    public static List<FieldMapping> parseColumnMapping(String columnMappingJson) {
        if (StrUtil.isBlank(columnMappingJson)) {
            return Collections.emptyList();
        }
        
        List<FieldMapping> result = new ArrayList<>();
        try {
            JSONArray mappings = JSONUtil.parseArray(columnMappingJson);
            
            for (int i = 0; i < mappings.size(); i++) {
                JSONObject item = mappings.getJSONObject(i);
                FieldMapping mapping = new FieldMapping();
                mapping.setExcelColumn(item.getStr("excelColumn"));
                mapping.setFieldName(item.getStr("fieldName"));
                mapping.setFieldType(item.getStr("fieldType"));
                mapping.setDateFormat(item.getStr("dateFormat"));
                Object scaleObj = item.get("scale");
                if (scaleObj != null) {
                    mapping.setScale(Integer.parseInt(scaleObj.toString()));
                }

                if (item.containsKey("cleanRules") && item.get("cleanRules") != null) {
                    JSONArray rules = item.getJSONArray("cleanRules");
                    List<CleanRule> cleanRules = new ArrayList<>();
                    for (int j = 0; j < rules.size(); j++) {
                        JSONObject rule = rules.getJSONObject(j);
                        CleanRule cr = new CleanRule();
                        cr.setPattern(rule.getStr("pattern"));
                        cr.setReplace(rule.getStr("replace"));
                        cleanRules.add(cr);
                    }
                    mapping.setCleanRules(cleanRules);
                }

                if (item.containsKey("validators") && item.get("validators") != null) {
                    JSONObject validatorsObj = item.getJSONObject("validators");
                    Validators validators = new Validators();
                    validators.setRequired(validatorsObj.getBool("required"));
                    validators.setPositiveOnly(validatorsObj.getBool("positiveOnly"));
                    validators.setMinValue(validatorsObj.getStr("minValue"));
                    validators.setMaxValue(validatorsObj.getStr("maxValue"));
                    mapping.setValidators(validators);
                }

                result.add(mapping);
            }
        } catch (Exception e) {
            log.error("解析列映射配置失败: {}", columnMappingJson, e);
        }
        
        return result;
    }
}
