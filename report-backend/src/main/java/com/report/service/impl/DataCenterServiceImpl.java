package com.report.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.report.entity.TableLayerMapping;
import com.report.mapper.TableLayerMappingMapper;
import com.report.service.DataCenterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
public class DataCenterServiceImpl implements DataCenterService {

    @Autowired
    private TableLayerMappingMapper tableLayerMappingMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final Pattern SAFE_SQL_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\s=<>('%)\\.,]+$");

    @Override
    public List<TableLayerMapping> listTables(String tableLayer, String sourceType, String businessDomain) {
        LambdaQueryWrapper<TableLayerMapping> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(tableLayer)) {
            wrapper.eq(TableLayerMapping::getTableLayer, tableLayer);
        }
        if (StrUtil.isNotBlank(sourceType)) {
            wrapper.eq(TableLayerMapping::getSourceType, sourceType);
        }
        if (StrUtil.isNotBlank(businessDomain)) {
            wrapper.like(TableLayerMapping::getBusinessDomain, businessDomain);
        }
        wrapper.orderByDesc(TableLayerMapping::getUpdateTime);
        return tableLayerMappingMapper.selectList(wrapper);
    }

    @Override
    public TableLayerMapping getTableByName(String tableName) {
        return tableLayerMappingMapper.selectOne(
            new LambdaQueryWrapper<TableLayerMapping>().eq(TableLayerMapping::getTableName, tableName)
        );
    }

    @Override
    @Transactional
    public boolean updateTableMapping(TableLayerMapping mapping) {
        if (mapping.getId() != null) {
            return tableLayerMappingMapper.updateById(mapping) > 0;
        } else {
            mapping.setMarked(1);
            return tableLayerMappingMapper.insert(mapping) > 0;
        }
    }

    @Override
    public List<TableLayerMapping> listUntaggedTables() {
        return tableLayerMappingMapper.selectList(
            new LambdaQueryWrapper<TableLayerMapping>().eq(TableLayerMapping::getMarked, 0)
        );
    }

    @Override
    @Transactional
    public List<String> scanNewTables() {
        String sql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES " +
                     "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE' " +
                     "AND TABLE_NAME NOT IN (" +
                     "  SELECT table_name FROM table_layer_mapping" +
                     ") " +
                     "AND (" +
                     "  TABLE_NAME LIKE 'ods\\_%' OR " +
                     "  TABLE_NAME LIKE 'dwd\\_%' OR " +
                     "  TABLE_NAME LIKE 'dws\\_%' OR " +
                     "  TABLE_NAME LIKE 'ads\\_%' OR " +
                     "  TABLE_NAME LIKE 'dim\\_%' OR " +
                     "  TABLE_NAME LIKE 'mid\\_%' OR " +
                     "  TABLE_NAME LIKE 'tmp\\_%'" +
                     ")";
        List<String> newTables = jdbcTemplate.queryForList(sql, String.class);

        for (String tableName : newTables) {
            TableLayerMapping mapping = new TableLayerMapping();
            mapping.setTableName(tableName);
            mapping.setMarked(0);
            tableLayerMappingMapper.insert(mapping);
        }
        return newTables;
    }

    @Override
    public Map<String, Object> getTableData(String tableName, Integer pageNum, Integer pageSize, String condition) {
        if (!isValidTableName(tableName)) {
            throw new IllegalArgumentException("无效的表名");
        }

        pageNum = pageNum != null ? pageNum : 1;
        pageSize = pageSize != null ? pageSize : 20;
        int offset = (pageNum - 1) * pageSize;

        String countSql = "SELECT COUNT(*) FROM " + tableName;
        if (StrUtil.isNotBlank(condition)) {
            if (!isValidCondition(condition)) {
                throw new IllegalArgumentException("无效的筛选条件");
            }
            countSql += " WHERE " + condition;
        }
        Long total = jdbcTemplate.queryForObject(countSql, Long.class);

        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(tableName);
        if (StrUtil.isNotBlank(condition)) {
            sql.append(" WHERE ").append(condition);
        }
        sql.append(" LIMIT ").append(pageSize).append(" OFFSET ").append(offset);

        List<Map<String, Object>> records = jdbcTemplate.queryForList(sql.toString());

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("size", pageSize);
        result.put("current", pageNum);
        result.put("pages", (total + pageSize - 1) / pageSize);
        return result;
    }

    @Override
    public List<Map<String, Object>> getTableColumns(String tableName) {
        if (!isValidTableName(tableName)) {
            throw new IllegalArgumentException("无效的表名");
        }
        String sql = "SELECT COLUMN_NAME as columnName, DATA_TYPE as dataType, COLUMN_COMMENT as columnComment " +
                     "FROM INFORMATION_SCHEMA.COLUMNS " +
                     "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? " +
                     "ORDER BY ORDINAL_POSITION";
        return jdbcTemplate.queryForList(sql, tableName);
    }

    private boolean isValidTableName(String tableName) {
        return tableName != null && tableName.matches("^[a-zA-Z][a-zA-Z0-9_]*$");
    }

    private boolean isValidCondition(String condition) {
        if (condition == null || condition.length() > 500) {
            return false;
        }
        return SAFE_SQL_PATTERN.matcher(condition).matches();
    }
}