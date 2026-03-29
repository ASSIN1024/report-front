package com.report.util;

import com.report.entity.dto.ColumnMapping;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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
                    return getCellValue(cell.getCachedFormulaResultValue(), mapping);
                    
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
}
