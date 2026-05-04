package com.report.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.report.service.OdsBackupService;
import com.report.service.TransformResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

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
        for (int i = 0; i < colNames.size(); i++) {
            createSql.append("`").append(colNames.get(i)).append("` varchar(500) DEFAULT NULL");
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
        if (fieldMappingJson != null && !fieldMappingJson.isEmpty()) {
            try {
                Map<String, Object> fieldMap = objectMapper.readValue(fieldMappingJson, Map.class);
                if (fieldMap != null && !fieldMap.isEmpty()) {
                    List<String> names = new java.util.ArrayList<>(fieldMap.keySet());
                    log.info("Extracted column names from fieldMappingJson: {}", names);
                    return names;
                }
            } catch (Exception e) {
                log.warn("Failed to parse fieldMappingJson, using numbered columns", e);
            }
        }

        List<String> headers = result.getHeaders();
        if (headers != null && !headers.isEmpty()) {
            List<String> names = new java.util.ArrayList<>();
            java.util.Set<String> used = new java.util.HashSet<>();
            for (int i = 0; i < headers.size(); i++) {
                String sanitized = sanitizeColumnName(headers.get(i));
                String unique = sanitized;
                int suffix = 1;
                while (used.contains(unique)) {
                    unique = sanitized + "_" + suffix;
                    suffix++;
                }
                used.add(unique);
                names.add(unique);
            }
            return names;
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

    private String sanitizeColumnName(String name) {
        if (name == null || name.isEmpty()) {
            return "unknown_col";
        }
        return name.replaceAll("[^a-zA-Z0-9_]", "_").replaceAll("^\\d", "_");
    }
}
