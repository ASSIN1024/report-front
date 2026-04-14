package com.report.pipeline;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.report.entity.dto.StepContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.time.LocalDate;
import java.util.List;

@Slf4j
public abstract class MyBatisPlusAbstractStep implements PipelineStep {

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Override
    public void execute(StepContext context) throws StepExecutionException {
        TransactionStatus status = transactionManager.getTransaction(
            new DefaultTransactionDefinition()
        );

        try {
            if (isIdempotent()) {
                log.info("[{}] 幂等模式执行", getStepName());
                doExecute(context);
            } else {
                if (isOverwrite()) {
                    clearPartition(getTargetTable(), context.getPartitionDate());
                }
                doExecute(context);
            }
            transactionManager.commit(status);
            log.info("[{}] 执行成功", getStepName());
        } catch (Exception e) {
            transactionManager.rollback(status);
            log.error("[{}] 执行失败，已回滚: {}", getStepName(), e.getMessage());
            throw new StepExecutionException(getStepName(), "Step执行失败", e);
        }
    }

    @Override
    public boolean isOverwrite() {
        return true;
    }

    @Override
    public boolean isIdempotent() {
        return false;
    }

    protected void clearPartition(String tableName, LocalDate partitionDate) {
        String safeTableName = tableName.replaceAll("[^a-zA-Z0-9_]", "");
        String sql = String.format("DELETE FROM %s WHERE pt_dt = ?", safeTableName);
        jdbcTemplate.update(sql, partitionDate.toString());
        log.info("[{}] 清空分区: {} pt_dt={}", getStepName(), safeTableName, partitionDate);
    }

    protected <T> void insertBatch(BaseMapper<T> mapper, List<T> dataList) {
        insertBatch(mapper, dataList, 500);
    }

    protected <T> void insertBatch(BaseMapper<T> mapper, List<T> dataList, int batchSize) {
        if (dataList == null || dataList.isEmpty()) {
            return;
        }

        if (isIdempotent()) {
            insertBatchUpsert(mapper, dataList, batchSize);
        } else {
            insertBatchPlain(mapper, dataList, batchSize);
        }
    }

    private <T> void insertBatchPlain(BaseMapper<T> mapper, List<T> dataList, int batchSize) {
        for (int i = 0; i < dataList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, dataList.size());
            List<T> batch = dataList.subList(i, end);

            for (T entity : batch) {
                mapper.insert(entity);
            }
        }
        log.info("[{}] 批量插入完成，共{}条", getStepName(), dataList.size());
    }

    private <T> void insertBatchUpsert(BaseMapper<T> mapper, List<T> dataList, int batchSize) {
        String tableName = getTargetTable();
        Class<?> entityClass = dataList.get(0).getClass();

        for (T entity : dataList) {
            String[] columns = getEntityColumns(entity);
            String[] values = getEntityValues(entity);

            String sql = buildUpsertSql(tableName, columns, values);
            jdbcTemplate.update(sql);
        }
        log.info("[{}] 幂等批量插入完成，共{}条", getStepName(), dataList.size());
    }

    private String buildUpsertSql(String tableName, String[] columns, String[] values) {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(tableName).append(" (");
        sql.append(String.join(", ", columns));
        sql.append(") VALUES (");
        sql.append(String.join(", ", values));
        sql.append(") ON DUPLICATE KEY UPDATE ");

        String[] updateParts = new String[columns.length];
        for (int i = 0; i < columns.length; i++) {
            updateParts[i] = columns[i] + " = VALUES(" + columns[i] + ")";
        }
        sql.append(String.join(", ", updateParts));

        return sql.toString();
    }

    private <T> String[] getEntityColumns(T entity) {
        java.lang.reflect.Field[] fields = entity.getClass().getDeclaredFields();
        java.util.List<String> columns = new java.util.ArrayList<>();
        for (java.lang.reflect.Field field : fields) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            String columnName = convertToSnakeCase(field.getName());
            if (!"id".equals(columnName) || isIdAutoIncrement(entity)) {
                columns.add(columnName);
            }
        }
        return columns.toArray(new String[0]);
    }

    private <T> String[] getEntityValues(T entity) {
        java.lang.reflect.Field[] fields = entity.getClass().getDeclaredFields();
        java.util.List<String> values = new java.util.ArrayList<>();
        for (java.lang.reflect.Field field : fields) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (!"id".equals(field.getName()) || isIdAutoIncrement(entity)) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(entity);
                    values.add(formatValue(value));
                } catch (IllegalAccessException e) {
                    values.add("NULL");
                }
            }
        }
        return values.toArray(new String[0]);
    }

    private <T> boolean isIdAutoIncrement(T entity) {
        try {
            java.lang.reflect.Field idField = entity.getClass().getDeclaredField("id");
            return idField.isAnnotationPresent(com.baomidou.mybatisplus.annotation.IdType.class)
                   && idField.getAnnotation(com.baomidou.mybatisplus.annotation.IdType.class).value() == com.baomidou.mybatisplus.annotation.IdType.AUTO;
        } catch (NoSuchFieldException e) {
            return true;
        }
    }

    private String convertToSnakeCase(String camelCase) {
        return camelCase.replaceAll("([A-Z])", "_$1").toLowerCase().replaceAll("^_", "");
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof String) {
            return "'" + ((String) value).replace("'", "''") + "'";
        }
        if (value instanceof java.util.Date) {
            return "'" + new java.text.SimpleDateFormat("yyyy-MM-dd").format(value) + "'";
        }
        if (value instanceof java.time.LocalDate) {
            return "'" + value + "'";
        }
        if (value instanceof java.time.LocalDateTime) {
            return "'" + value + "'";
        }
        if (value instanceof java.math.BigDecimal) {
            return ((java.math.BigDecimal) value).toPlainString();
        }
        return value.toString();
    }

    protected abstract String getTableName();
    protected abstract void doExecute(StepContext context) throws StepExecutionException;

    @Override
    public String getTargetTable() {
        return getTableName();
    }
}
