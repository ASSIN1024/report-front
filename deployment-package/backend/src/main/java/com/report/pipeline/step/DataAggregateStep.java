package com.report.pipeline.step;

import com.report.entity.dws.Layer2Summary;
import com.report.entity.dto.StepContext;
import com.report.mapper.Layer1SalesMapper;
import com.report.mapper.Layer2SummaryMapper;
import com.report.pipeline.MyBatisPlusAbstractStep;
import com.report.pipeline.StepExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class DataAggregateStep extends MyBatisPlusAbstractStep {

    @Autowired
    private Layer1SalesMapper layer1SalesMapper;

    @Autowired
    private Layer2SummaryMapper layer2SummaryMapper;

    @Override
    public String getStepName() {
        return "数据聚合";
    }

    @Override
    protected String getTableName() {
        return "layer_2_summary";
    }

    @Override
    public boolean isIdempotent() {
        return true;
    }

    @Override
    protected void doExecute(StepContext context) throws StepExecutionException {
        LocalDate partitionDate = context.getPartitionDate();
        Date partitionDateAsDate = Date.from(partitionDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());

        log.info("[数据聚合] 从 DWD 表读取并聚合数据，分区: {}", partitionDate);

        List<Layer2Summary> aggregatedData = layer1SalesMapper.aggregateByProduct(partitionDateAsDate);

        aggregatedData.forEach(row -> {
            if (row.getTotalAmount() == null) {
                row.setTotalAmount(BigDecimal.ZERO);
            }
            if (row.getOrderCount() == null) {
                row.setOrderCount(0);
            }
        });

        log.info("[数据聚合] 聚合得到 {} 行数据", aggregatedData.size());

        insertBatch(layer2SummaryMapper, aggregatedData);
        log.info("[数据聚合] 完成，写入 {} 行到 {}", aggregatedData.size(), getTargetTable());
    }
}
