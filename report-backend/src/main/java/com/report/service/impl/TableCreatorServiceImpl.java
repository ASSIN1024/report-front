package com.report.service.impl;

import cn.hutool.core.util.StrUtil;
import com.report.entity.dto.FieldMapping;
import com.report.service.TableCreatorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class TableCreatorServiceImpl implements TableCreatorService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public boolean checkTableExists(String tableName) {
        String sql = "SELECT COUNT(*) FROM information_schema.TABLES " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName);
        return count != null && count > 0;
    }

    @Override
    public void createTable(String tableName, List<FieldMapping> columnMappings) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS ").append(tableName).append(" (\n");
        sql.append("    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',\n");
        sql.append("    pt_dt DATE NOT NULL COMMENT '分区日期',\n");
        
        for (FieldMapping mapping : columnMappings) {
            String columnDef = buildColumnDefinition(mapping);
            sql.append("    ").append(mapping.getFieldName()).append(" ").append(columnDef).append(",\n");
        }
        
        sql.append("    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',\n");
        sql.append("    INDEX idx_pt_dt (pt_dt)\n");
        sql.append(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自动创建的数据表'");
        
        log.info("创建数据表: {}", tableName);
        log.debug("建表SQL: {}", sql.toString());
        
        jdbcTemplate.execute(sql.toString());
        log.info("数据表创建成功: {}", tableName);
    }

    @Override
    public void ensureTableExists(String tableName, List<FieldMapping> columnMappings) {
        if (!checkTableExists(tableName)) {
            log.info("目标表不存在，自动创建: {}", tableName);
            createTable(tableName, columnMappings);
        } else {
            log.info("目标表已存在，检查是否需要添加缺失字段: {}", tableName);
            addMissingColumns(tableName, columnMappings);
        }
    }

    @Override
    public void addMissingColumns(String tableName, List<FieldMapping> columnMappings) {
        Set<String> existingColumns = getExistingColumns(tableName);
        
        for (FieldMapping mapping : columnMappings) {
            String fieldName = mapping.getFieldName();
            if (!existingColumns.contains(fieldName.toLowerCase())) {
                String columnDef = buildColumnDefinition(mapping);
                String sql = String.format("ALTER TABLE %s ADD COLUMN %s %s", 
                        tableName, fieldName, columnDef);
                log.info("添加缺失字段: {} -> {}", tableName, fieldName);
                jdbcTemplate.execute(sql);
            }
        }
    }

    private Set<String> getExistingColumns(String tableName) {
        String sql = "SELECT COLUMN_NAME FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?";
        List<String> columns = jdbcTemplate.queryForList(sql, String.class, tableName);
        Set<String> result = new HashSet<>();
        for (String col : columns) {
            result.add(col.toLowerCase());
        }
        return result;
    }

    private String buildColumnDefinition(FieldMapping mapping) {
        String fieldType = mapping.getFieldType();
        if (StrUtil.isBlank(fieldType)) {
            fieldType = "STRING";
        }
        
        StringBuilder def = new StringBuilder();
        
        switch (fieldType.toUpperCase()) {
            case "STRING":
                def.append("VARCHAR(255)");
                break;
            case "INTEGER":
                def.append("BIGINT");
                break;
            case "DECIMAL":
                int scale = mapping.getScale() != null ? mapping.getScale() : 2;
                def.append("DECIMAL(15,").append(scale).append(")");
                break;
            case "DATE":
                def.append("DATE");
                break;
            case "DATETIME":
                def.append("DATETIME");
                break;
            default:
                def.append("VARCHAR(255)");
        }
        
        def.append(" COMMENT '").append(mapping.getFieldName()).append("'");
        
        return def.toString();
    }
}
