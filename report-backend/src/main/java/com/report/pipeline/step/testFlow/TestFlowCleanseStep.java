package com.report.pipeline.step.testFlow;

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
public class TestFlowCleanseStep extends AbstractStep {
    @Override

    public String getStepName() {
        // TODO Auto-generated method stub
        return "TestFlow清洗";

    }

    @Override
    protected String getTableName() {
        // TODO Auto-generated method stub
        return "dwd_clean_tets_flow";

    }

    @Override
    protected void doExecute(StepContext context) throws StepExecutionException {
        LocalDate partitionDate = context.getPartitionDate();
        log.info("[TestFlow清洗] 从 test_flow 读取数据，分区: {}", partitionDate);

        List<Map<String, Object>> rawData = jdbcTemplate.queryForList(
                "SELECT name, amount, pt_dt FROM test_flow WHERE pt_dt = ?",
                java.sql.Date.valueOf(partitionDate));

        log.info("[TestFlow清洗] 读取到 {} 行数据", rawData.size());

        // 不需要再设置 pt_dt，insertData 方法会单独处理

        insertData(getTargetTable(), rawData, partitionDate);
        log.info("[TestFlow清洗] 完成，写入 {} 行到 {}", rawData.size(), getTargetTable());
    }

}
