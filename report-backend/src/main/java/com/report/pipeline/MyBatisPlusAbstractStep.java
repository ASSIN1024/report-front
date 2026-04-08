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
            if (isOverwrite()) {
                clearPartition(getTargetTable(), context.getPartitionDate());
            }
            doExecute(context);
            transactionManager.commit(status);
            log.info("[{}] 执行成功", getStepName());
        } catch (Exception e) {
            transactionManager.rollback(status);
            log.error("[{}] 执行失败，已回滚: {}", getStepName(), e.getMessage());
            throw new StepExecutionException(getStepName(), "Step执行失败", e);
        }
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

        for (int i = 0; i < dataList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, dataList.size());
            List<T> batch = dataList.subList(i, end);

            for (T entity : batch) {
                mapper.insert(entity);
            }
        }
        log.info("[{}] 批量插入完成，共{}条", getStepName(), dataList.size());
    }

    protected abstract String getTableName();
    protected abstract void doExecute(StepContext context) throws StepExecutionException;

    @Override
    public String getTargetTable() {
        return getTableName();
    }

    @Override
    public boolean isOverwrite() {
        return true;
    }
}
