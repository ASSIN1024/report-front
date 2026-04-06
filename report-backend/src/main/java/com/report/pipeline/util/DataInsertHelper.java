package com.report.pipeline.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
public class DataInsertHelper {

    public static void insertData(JdbcTemplate jdbcTemplate, String tableName, List<Map<String, Object>> dataList, LocalDate partitionDate) {
        if (dataList == null || dataList.isEmpty()) {
            log.info("[{}] 无数据可插入", tableName);
            return;
        }

        Map<String, Object> firstRow = dataList.get(0);
        StringBuilder columns = new StringBuilder("pt_dt");
        StringBuilder placeholders = new StringBuilder("?");

        for (String key : firstRow.keySet()) {
            if (!"pt_dt".equals(key)) {
                columns.append(", ").append(key);
                placeholders.append(", ?");
            }
        }

        String sql = String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, columns, placeholders);

        for (Map<String, Object> row : dataList) {
            try {
                java.sql.Date ptDt = java.sql.Date.valueOf(partitionDate);
                jdbcTemplate.update(sql, ptDt);
            } catch (Exception e) {
                log.error("插入数据失败: {}", row, e);
            }
        }
    }
}