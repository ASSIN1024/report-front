package com.report.pipeline.step;

import com.report.entity.dto.StepContext;
import com.report.pipeline.AbstractStep;
import com.report.pipeline.StepExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class DataCleanseStep extends AbstractStep {

    @Override
    public String getStepName() {
        return "数据清洗";
    }

    @Override
    protected String getTargetTable() {
        return "layer_1_sales";
    }

    @Override
    protected void doExecute(StepContext context) throws StepExecutionException {
        log.info("[数据清洗] 从 OSD 表读取数据，分区: {}", context.getPartitionDate());

        List<Map<String, Object>> rawData = jdbcTemplate.queryForList(
            "SELECT * FROM osd_sales WHERE pt_dt = ?",
            java.sql.Date.valueOf(context.getPartitionDate())
        );

        log.info("[数据清洗] 读取到 {} 行数据", rawData.size());

        for (Map<String, Object> row : rawData) {
            cleanseRow(row);
            row.put("pt_dt", context.getPartitionDate());
        }

        insertData(getTargetTable(), rawData, context.getPartitionDate());
        log.info("[数据清洗] 完成，写入 {} 行到 {}", rawData.size(), getTargetTable());
    }

    private void cleanseRow(Map<String, Object> row) {
        if (row.get("amount") == null || "".equals(row.get("amount"))) {
            row.put("amount", BigDecimal.ZERO);
        }
    }
}