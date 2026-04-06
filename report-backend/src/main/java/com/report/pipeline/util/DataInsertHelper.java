package com.report.pipeline.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class DataInsertHelper {

    public static void insertData(JdbcTemplate jdbcTemplate, String tableName, List<Map<String, Object>> dataList, LocalDate partitionDate) {
        if (dataList == null || dataList.isEmpty()) {
            log.info("[{}] 无数据可插入", tableName);
            return;
        }

        log.info("[{}] 开始插入 {} 行数据", tableName, dataList.size());

        for (Map<String, Object> row : dataList) {
            try {
                String columns = row.keySet().stream()
                    .filter(k -> !"pt_dt".equals(k))
                    .collect(Collectors.joining(", "));

                String values = row.keySet().stream()
                    .filter(k -> !"pt_dt".equals(k))
                    .map(k -> formatValue(row.get(k)))
                    .collect(Collectors.joining(", "));

                String sql = String.format(
                    "INSERT INTO %s (pt_dt, %s) VALUES ('%s', %s)",
                    tableName, columns, partitionDate, values
                );

                jdbcTemplate.execute(sql);
                log.debug("[{}] 插入行: {}", tableName, row);
            } catch (Exception e) {
                log.error("[{}] 插入数据失败: {}, error: {}", tableName, row, e.getMessage());
            }
        }

        log.info("[{}] 完成插入 {} 行数据", tableName, dataList.size());
    }

    private static String formatValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof String) {
            return "'" + ((String) value).replace("'", "''") + "'";
        }
        if (value instanceof BigDecimal) {
            return value.toString();
        }
        if (value instanceof Number) {
            return value.toString();
        }
        return "'" + value.toString().replace("'", "''") + "'";
    }
}
