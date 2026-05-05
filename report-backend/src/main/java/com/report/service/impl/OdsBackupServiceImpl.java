package com.report.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.report.service.OdsBackupService;
import com.report.service.TransformResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class OdsBackupServiceImpl implements OdsBackupService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void backup(TransformResult result, String sourceFileName) {
        if (sourceFileName == null || sourceFileName.isEmpty()) {
            log.warn("Source file not provided, skipping ODS backup");
            return;
        }

        String tableName = result.getTableName();
        if (!StringUtils.hasText(tableName)) {
            log.warn("ODS table name not configured, skipping backup");
            return;
        }

        try {
            if (!tableExists(tableName)) {
                createOdsTable(tableName, result);
            } else if (!isTableSchemaValid(tableName, result)) {
                log.warn("Table schema mismatch, recreating table: {}", tableName);
                dropTable(tableName);
                createOdsTable(tableName, result);
            }

            insertOdsData(tableName, result, sourceFileName);

            String sql = "INSERT INTO ods_backup (source_file, pt_dt, db_name, table_name, file_size, create_time) VALUES (?, ?, ?, ?, ?, NOW())";
            jdbcTemplate.update(sql,
                sourceFileName,
                result.getPtDt(),
                result.getDbName(),
                tableName,
                result.getFileSize()
            );

            log.info("ODS backup completed: file={}, pt_dt={}, db={}, table={}, rows={}",
                sourceFileName, result.getPtDt(), result.getDbName(), tableName,
                result.getRows() != null ? result.getRows().size() : 0);
        } catch (Exception e) {
            log.error("ODS backup failed: file={}, table={}", sourceFileName, tableName, e);
            throw e;
        }
    }

    private boolean isTableSchemaValid(String tableName, TransformResult result) {
        try {
            List<String> colNames = extractColumnNames(result);

            String sql = "SELECT COLUMN_NAME FROM information_schema.columns WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME NOT IN ('id', 'source_file', 'pt_dt', 'create_time')";
            List<Map<String, Object>> columns = jdbcTemplate.queryForList(sql, tableName);

            if (columns.size() != colNames.size()) {
                log.warn("Column count mismatch: expected {}, found {}", colNames.size(), columns.size());
                return false;
            }

            Set<String> existingCols = new HashSet<>();
            for (Map<String, Object> col : columns) {
                existingCols.add((String) col.get("COLUMN_NAME"));
            }

            for (String expectedCol : colNames) {
                if (!existingCols.contains(expectedCol)) {
                    log.warn("Expected column '{}' not found in table", expectedCol);
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.warn("Failed to validate table schema: {}", tableName, e);
            return false;
        }
    }

    private void dropTable(String tableName) {
        try {
            jdbcTemplate.execute("DROP TABLE IF EXISTS `" + tableName + "`");
            log.info("Dropped table: {}", tableName);
        } catch (Exception e) {
            log.error("Failed to drop table: {}", tableName, e);
            throw new RuntimeException("Failed to drop table: " + tableName, e);
        }
    }

    private boolean tableExists(String tableName) {
        try {
            String sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName);
            return count != null && count > 0;
        } catch (Exception e) {
            log.warn("Check table existence failed: {}", tableName, e);
            return false;
        }
    }

    private void createOdsTable(String tableName, TransformResult result) {
        StringBuilder createSql = new StringBuilder();
        createSql.append("CREATE TABLE IF NOT EXISTS `").append(tableName).append("` (");
        createSql.append("id bigint NOT NULL AUTO_INCREMENT, ");
        createSql.append("source_file varchar(200) DEFAULT NULL COMMENT '源文件名', ");
        createSql.append("pt_dt varchar(20) DEFAULT NULL COMMENT '分区日期', ");
        createSql.append("create_time datetime DEFAULT CURRENT_TIMESTAMP, ");

        List<String> colNames = extractColumnNames(result);
        Map<String, String> fieldTypes = extractFieldTypes(result);

        for (int i = 0; i < colNames.size(); i++) {
            String colName = colNames.get(i);
            String sqlType = fieldTypes.get(colName);
            if (sqlType == null) {
                sqlType = "varchar(500)";
            } else {
                sqlType = convertToSqlType(sqlType);
            }
            createSql.append("`").append(colName).append("` ").append(sqlType).append(" DEFAULT NULL");
            if (i < colNames.size() - 1) {
                createSql.append(", ");
            }
        }

        createSql.append(", PRIMARY KEY (`id`)");
        createSql.append(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci");

        try {
            jdbcTemplate.execute(createSql.toString());
            log.info("Created ODS table: {}", tableName);
        } catch (Exception e) {
            log.error("Failed to create ODS table: {}. SQL: {}", tableName, createSql, e);
            throw new RuntimeException("Failed to create ODS table: " + tableName, e);
        }
    }

    private Map<String, String> extractFieldTypes(TransformResult result) {
        Map<String, String> fieldTypes = new HashMap<>();
        String fieldMappingJson = result.getFieldMappingJson();
        if (fieldMappingJson != null && !fieldMappingJson.isEmpty()) {
            try {
                Map<String, Map<String, String>> fieldMap = objectMapper.readValue(fieldMappingJson,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Map<String, String>>>() {});
                for (Map.Entry<String, Map<String, String>> entry : fieldMap.entrySet()) {
                    String fieldName = entry.getKey();
                    Map<String, String> fieldInfo = entry.getValue();
                    String type = fieldInfo.get("type");
                    if (type != null) {
                        fieldTypes.put(fieldName, type);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse field types from fieldMappingJson", e);
            }
        }
        return fieldTypes;
    }

    private String convertToSqlType(String fieldType) {
        if (fieldType == null) {
            return "varchar(500)";
        }
        switch (fieldType.toUpperCase()) {
            case "INTEGER":
            case "INT":
                return "bigint";
            case "DECIMAL":
            case "FLOAT":
            case "DOUBLE":
                return "decimal(20,4)";
            case "DATE":
                return "date";
            case "DATETIME":
                return "datetime";
            case "BOOLEAN":
                return "tinyint(1)";
            default:
                return "varchar(500)";
        }
    }

    private void insertOdsData(String tableName, TransformResult result, String sourceFileName) {
        List<Map<String, Object>> rows = result.getRows();
        if (rows == null || rows.isEmpty()) {
            log.info("No data rows to insert for ODS backup");
            return;
        }

        java.sql.Date sqlDate = null;
        if (result.getPtDt() != null) {
            try {
                sqlDate = java.sql.Date.valueOf(result.getPtDt());
            } catch (Exception e) {
                log.warn("Invalid pt_dt format: {}", result.getPtDt());
            }
        }

        List<String> colNames = extractColumnNames(result);
        List<String> headers = result.getHeaders();

        for (Map<String, Object> row : rows) {
            StringBuilder sql = new StringBuilder();
            StringBuilder placeholders = new StringBuilder();

            sql.append("INSERT INTO `").append(tableName).append("` (source_file, pt_dt, create_time");
            placeholders.append("?, ?, NOW()");

            for (String colName : colNames) {
                sql.append(", `").append(colName).append("`");
                placeholders.append(", ?");
            }

            sql.append(") VALUES (").append(placeholders).append(")");

            List<Object> params = new java.util.ArrayList<>();
            params.add(sourceFileName);
            params.add(sqlDate);

            for (int i = 0; i < colNames.size(); i++) {
                Object value = headers != null && i < headers.size() ? row.get(headers.get(i)) : null;
                params.add(value != null ? value.toString() : null);
            }

            jdbcTemplate.update(sql.toString(), params.toArray());
        }

        log.info("Inserted {} rows into ODS table: {}", rows.size(), tableName);
    }

    private List<String> extractColumnNames(TransformResult result) {
        String fieldMappingJson = result.getFieldMappingJson();
        List<String> fieldMappingNames = null;

        if (fieldMappingJson != null && !fieldMappingJson.isEmpty()) {
            try {
                Map<String, Object> fieldMap = objectMapper.readValue(fieldMappingJson, Map.class);
                if (fieldMap != null && !fieldMap.isEmpty()) {
                    fieldMappingNames = new java.util.ArrayList<>(fieldMap.keySet());
                }
            } catch (Exception e) {
                log.warn("Failed to parse fieldMappingJson", e);
            }
        }

        List<String> headers = result.getHeaders();
        if (fieldMappingNames != null && !fieldMappingNames.isEmpty() && !containsOnlyPlaceholderNames(fieldMappingNames)) {
            log.info("Extracted column names from fieldMappingJson: {}", fieldMappingNames);
            return fieldMappingNames;
        }

        if (headers != null && !headers.isEmpty()) {
            List<String> names = new java.util.ArrayList<>();
            java.util.Set<String> used = new java.util.HashSet<>();
            for (int i = 0; i < headers.size(); i++) {
                String sanitized = sanitizeColumnName(headers.get(i));
                if (sanitized == null || sanitized.isEmpty()) {
                    sanitized = "col_" + (i + 1);
                }
                String unique = sanitized;
                int suffix = 1;
                while (used.contains(unique)) {
                    unique = sanitized + "_" + suffix;
                    suffix++;
                }
                used.add(unique);
                names.add(unique);
            }
            log.info("Extracted column names from Excel headers: {}", names);
            return names;
        }

        if (fieldMappingNames != null && !fieldMappingNames.isEmpty()) {
            log.info("Extracted column names from fieldMappingJson (fallback): {}", fieldMappingNames);
            return fieldMappingNames;
        }

        if (result.getRows() != null && !result.getRows().isEmpty()) {
            Map<String, Object> firstRow = result.getRows().get(0);
            List<String> names = new java.util.ArrayList<>();
            int idx = 1;
            for (String key : firstRow.keySet()) {
                names.add("col_" + idx);
                idx++;
            }
            return names;
        }

        return java.util.Collections.emptyList();
    }

    private boolean containsOnlyPlaceholderNames(List<String> names) {
        for (String name : names) {
            if (name != null && !name.matches("^field_\\d+$") && !name.matches("^col_\\d+$")) {
                return false;
            }
        }
        return true;
    }

    private String sanitizeColumnName(String name) {
        if (name == null || name.isEmpty()) {
            return "unknown_col";
        }
        return name.replaceAll("[^a-zA-Z0-9_]", "_").replaceAll("^\\d", "_");
    }
}
