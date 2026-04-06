package com.report.pipeline;

import com.report.entity.dto.StepContext;
import com.report.pipeline.util.DataInsertHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
public abstract class AbstractStep implements PipelineStep {

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Override
    public void execute(StepContext context) throws StepExecutionException {
        if (isOverwrite()) {
            clearPartition(getTargetTable(), context.getPartitionDate());
        }
        doExecute(context);
    }

    protected void clearPartition(String tableName, LocalDate partitionDate) {
        String sql = String.format("DELETE FROM %s WHERE pt_dt = '%s'", tableName, partitionDate);
        log.info("[{}] 清空分区: {}", getStepName(), sql);
        jdbcTemplate.execute(sql);
    }

    // Insert data using helper to avoid code duplication
    protected void insertData(String tableName, List<Map<String, Object>> dataList, LocalDate partitionDate) {
        DataInsertHelper.insertData(jdbcTemplate, tableName, dataList, partitionDate);
    }

    protected abstract void doExecute(StepContext context) throws StepExecutionException;

    protected abstract String getTargetTable();

    @Override
    public boolean isOverwrite() {
        return true;
    }
}