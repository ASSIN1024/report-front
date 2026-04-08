package com.report.pipeline.step.testFlow;

import com.report.entity.dws.DwdTestFlowAgg;
import com.report.entity.dto.StepContext;
import com.report.mapper.DwdCleanTestFlowMapper;
import com.report.mapper.DwdTestFlowAggMapper;
import com.report.pipeline.MyBatisPlusAbstractStep;
import com.report.pipeline.StepExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
public class TestFlowAggregateStep extends MyBatisPlusAbstractStep {

    @Autowired
    private DwdCleanTestFlowMapper dwdCleanTestFlowMapper;

    @Autowired
    private DwdTestFlowAggMapper dwdTestFlowAggMapper;

    @Override
    public String getStepName() {
        return "TestFlow聚合";
    }

    @Override
    protected String getTableName() {
        return "dwd_tets_flow_agg";
    }

    @Override
    protected void doExecute(StepContext context) throws StepExecutionException {
        LocalDate partitionDate = context.getPartitionDate();

        log.info("[TestFlow聚合] 从 dwd_clean_tets_flow 读取并聚合数据，分区: {}", partitionDate);

        List<DwdTestFlowAgg> aggregatedData = dwdCleanTestFlowMapper.aggregateByName(partitionDate);

        aggregatedData.forEach(row -> {
            if (row.getTotalAmount() == null) {
                row.setTotalAmount(BigDecimal.ZERO);
            }
        });

        log.info("[TestFlow聚合] 聚合得到 {} 行数据", aggregatedData.size());

        insertBatch(dwdTestFlowAggMapper, aggregatedData);
        log.info("[TestFlow聚合] 完成，写入 {} 行到 {}", aggregatedData.size(), getTargetTable());
    }
}
