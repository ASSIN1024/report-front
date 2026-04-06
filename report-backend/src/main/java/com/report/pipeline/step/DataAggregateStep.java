package com.report.pipeline.step;

import com.report.entity.dto.StepContext;
import com.report.pipeline.AbstractStep;
import com.report.pipeline.StepExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class DataAggregateStep extends AbstractStep {

    @Override
    public String getStepName() {
        return "数据聚合";
    }

    @Override
    protected String getTargetTable() {
        return "layer_2_summary";
    }

    @Override
    protected void doExecute(StepContext context) throws StepExecutionException {
        LocalDate partitionDate = context.getPartitionDate();

        String sql = String.format(
            "SELECT product_name, SUM(amount) as total_amount, COUNT(*) as order_count " +
            "FROM layer_1_sales WHERE pt_dt = '%s' GROUP BY product_name",
            partitionDate
        );

        List<Map<String, Object>> aggregatedData = jdbcTemplate.queryForList(sql);

        for (Map<String, Object> row : aggregatedData) {
            row.put("pt_dt", partitionDate);
        }

        insertData(getTargetTable(), aggregatedData, partitionDate);
        log.info("[数据聚合] 完成，写入 {} 行到 {}", aggregatedData.size(), getTargetTable());
    }
}