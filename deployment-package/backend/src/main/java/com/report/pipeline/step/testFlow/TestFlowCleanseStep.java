package com.report.pipeline.step.testFlow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.report.entity.dwd.DwdCleanTestFlow;
import com.report.entity.ods.TestFlow;
import com.report.entity.dto.StepContext;
import com.report.mapper.DwdCleanTestFlowMapper;
import com.report.mapper.TestFlowMapper;
import com.report.pipeline.MyBatisPlusAbstractStep;
import com.report.pipeline.StepExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TestFlowCleanseStep extends MyBatisPlusAbstractStep {

    @Autowired
    private TestFlowMapper testFlowMapper;

    @Autowired
    private DwdCleanTestFlowMapper dwdCleanTestFlowMapper;

    @Override
    public String getStepName() {
        return "TestFlow清洗";
    }

    @Override
    protected String getTableName() {
        return "dwd_clean_tets_flow";
    }

    @Override
    protected void doExecute(StepContext context) throws StepExecutionException {
        LocalDate partitionDate = context.getPartitionDate();
        Date partitionDateAsDate = Date.from(partitionDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

        log.info("[TestFlow清洗] 从 test_flow 读取数据，分区: {}", partitionDate);

        List<TestFlow> rawData = testFlowMapper.selectList(
            new LambdaQueryWrapper<TestFlow>()
                .eq(TestFlow::getPtDt, partitionDateAsDate)
        );

        log.info("[TestFlow清洗] 读取到 {} 行数据", rawData.size());

        List<DwdCleanTestFlow> cleansedData = rawData.stream()
            .map(this::transformRow)
            .collect(Collectors.toList());

        insertBatch(dwdCleanTestFlowMapper, cleansedData);
        log.info("[TestFlow清洗] 完成，写入 {} 行到 {}", cleansedData.size(), getTargetTable());
    }

    private DwdCleanTestFlow transformRow(TestFlow rawRow) {
        DwdCleanTestFlow cleansed = new DwdCleanTestFlow();
        cleansed.setName(rawRow.getName());
        cleansed.setAmount(rawRow.getAmount());
        cleansed.setPtDt(rawRow.getPtDt());
        return cleansed;
    }
}
