package com.report.service.impl;

import cn.hutool.core.util.StrUtil;
import com.report.entity.dto.DataQueryDTO;
import com.report.service.DataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class DataServiceImpl implements DataService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public Map<String, Object> queryPage(DataQueryDTO queryDTO) {
        String tableName = queryDTO.getTableName();
        if (StrUtil.isBlank(tableName)) {
            throw new IllegalArgumentException("表名不能为空");
        }

        int pageNum = queryDTO.getPageNum() != null ? queryDTO.getPageNum() : 1;
        int pageSize = queryDTO.getPageSize() != null ? queryDTO.getPageSize() : 10;
        int offset = (pageNum - 1) * pageSize;

        String countSql = "SELECT COUNT(*) FROM " + tableName;
        if (StrUtil.isNotBlank(queryDTO.getCondition())) {
            countSql += " WHERE " + queryDTO.getCondition();
        }
        Long total = jdbcTemplate.queryForObject(countSql, Long.class);

        StringBuilder sql = new StringBuilder("SELECT ");
        if (queryDTO.getColumns() != null && !queryDTO.getColumns().isEmpty()) {
            sql.append(String.join(", ", queryDTO.getColumns()));
        } else {
            sql.append("*");
        }
        sql.append(" FROM ").append(tableName);
        if (StrUtil.isNotBlank(queryDTO.getCondition())) {
            sql.append(" WHERE ").append(queryDTO.getCondition());
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
    public List<Map<String, Object>> queryList(DataQueryDTO queryDTO) {
        String tableName = queryDTO.getTableName();
        if (StrUtil.isBlank(tableName)) {
            throw new IllegalArgumentException("表名不能为空");
        }

        StringBuilder sql = new StringBuilder("SELECT ");
        if (queryDTO.getColumns() != null && !queryDTO.getColumns().isEmpty()) {
            sql.append(String.join(", ", queryDTO.getColumns()));
        } else {
            sql.append("*");
        }
        sql.append(" FROM ").append(tableName);
        if (StrUtil.isNotBlank(queryDTO.getCondition())) {
            sql.append(" WHERE ").append(queryDTO.getCondition());
        }
        sql.append(" LIMIT 1000");

        return jdbcTemplate.queryForList(sql.toString());
    }

    @Override
    public List<String> getTableColumns(String tableName) {
        String sql = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? " +
                "ORDER BY ORDINAL_POSITION";
        return jdbcTemplate.queryForList(sql, String.class, tableName);
    }

    @Override
    public List<String> getOutputTables() {
        String sql = "SELECT DISTINCT output_table FROM report_config WHERE deleted = 0 AND output_table IS NOT NULL AND output_table != ''";
        return jdbcTemplate.queryForList(sql, String.class);
    }
}
