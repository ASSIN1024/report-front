package com.report.pipeline.step.testFlow;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.report.entity.dto.StepContext;
import com.report.pipeline.AbstractStep;
import com.report.pipeline.StepExecutionException;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TestFlowAggregateStep extends AbstractStep {

    @Override
    public String getStepName() {
        // TODO Auto-generated method stub
        return "TestFlow聚合";
    }

    @Override
    protected String getTableName() {
        // TODO Auto-generated method stub
        return "dwd_tets_flow_agg";
    }

    @Override
    protected void doExecute(StepContext context) throws StepExecutionException {
        LocalDate partitionDate = context.getPartitionDate();
        log.info("[TestFlow聚合] 从 dwd_clean_tets_flow 读取数据，分区: {}", partitionDate);
        String sql = String.format(
                "SELECT name, SUM(amount) as total_amount " +
                        "FROM dwd_clean_tets_flow WHERE pt_dt = '%s' GROUP BY name",
                partitionDate);
        List<Map<String, Object>> aggregatedData = jdbcTemplate.queryForList(sql);

        for (Map<String, Object> row : aggregatedData) {
            if (row.get("total_amount") == null) {
                row.put("total_amount", BigDecimal.ZERO);
            }
        }

        log.info("[TestFlow聚合] 读取到 {} 行数据", aggregatedData.size());
        insertData(getTargetTable(), aggregatedData, partitionDate);
        log.info("[TestFlow聚合] 完成，写入 {} 行到 {}", aggregatedData.size(), getTargetTable());

    }

}
