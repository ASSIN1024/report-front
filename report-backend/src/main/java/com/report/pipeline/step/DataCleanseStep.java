package com.report.pipeline.step;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.report.entity.dwd.Layer1Sales;
import com.report.entity.ods.OsdSales;
import com.report.entity.dto.StepContext;
import com.report.mapper.Layer1SalesMapper;
import com.report.mapper.OsdSalesMapper;
import com.report.pipeline.MyBatisPlusAbstractStep;
import com.report.pipeline.StepExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DataCleanseStep extends MyBatisPlusAbstractStep {

    @Autowired
    private OsdSalesMapper osdSalesMapper;

    @Autowired
    private Layer1SalesMapper layer1SalesMapper;

    @Override
    public String getStepName() {
        return "数据清洗";
    }

    @Override
    protected String getTableName() {
        return "layer_1_sales";
    }

    @Override
    protected void doExecute(StepContext context) throws StepExecutionException {
        LocalDate partitionDate = context.getPartitionDate();

        log.info("[数据清洗] 从 OSD 表读取数据，分区: {}", partitionDate);

        List<OsdSales> rawData = osdSalesMapper.selectList(
            new LambdaQueryWrapper<OsdSales>()
                .eq(OsdSales::getPtDt, partitionDate)
        );

        log.info("[数据清洗] 读取到 {} 行数据", rawData.size());

        List<Layer1Sales> cleansedData = rawData.stream()
            .map(this::cleanseRow)
            .collect(Collectors.toList());

        insertBatch(layer1SalesMapper, cleansedData);
        log.info("[数据清洗] 完成，写入 {} 行到 {}", cleansedData.size(), getTargetTable());
    }

    private Layer1Sales cleanseRow(OsdSales rawRow) {
        Layer1Sales cleansed = new Layer1Sales();
        cleansed.setProductName(rawRow.getProductName());

        if (rawRow.getAmount() == null || rawRow.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            cleansed.setAmount(BigDecimal.ZERO);
        } else {
            cleansed.setAmount(rawRow.getAmount());
        }

        cleansed.setPtDt(rawRow.getPtDt());
        return cleansed;
    }
}
