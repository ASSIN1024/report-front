package com.report.pipeline.step;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.report.entity.ods.OsdCompanyDeposit;
import com.report.entity.dto.StepContext;
import com.report.mapper.OsdCompanyDepositMapper;
import com.report.pipeline.MyBatisPlusAbstractStep;
import com.report.pipeline.StepExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class DepositUnitConvertStep extends MyBatisPlusAbstractStep {

    @Autowired
    private OsdCompanyDepositMapper osdCompanyDepositMapper;

    @Override
    public String getStepName() {
        return "单位转换";
    }

    @Override
    protected String getTableName() {
        return "osd_company_deposit";
    }

    @Override
    protected void doExecute(StepContext context) throws StepExecutionException {
        Date partitionDate = Date.from(context.getPartitionDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
        log.info("[单位转换] 读取 OSD 数据，分区: {}", context.getPartitionDate());

        LambdaQueryWrapper<OsdCompanyDeposit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OsdCompanyDeposit::getPtDt, partitionDate);
        wrapper.ne(OsdCompanyDeposit::getBranchCode, "合计");
        wrapper.or().isNull(OsdCompanyDeposit::getBranchCode);
        List<OsdCompanyDeposit> dataList = osdCompanyDepositMapper.selectList(wrapper);

        log.info("[单位转换] 读取到 {} 行数据，开始单位转换(元->万元)", dataList.size());

        for (OsdCompanyDeposit record : dataList) {
            if (record.getBalance() != null) {
                record.setBalanceWan(record.getBalance().divide(new BigDecimal("10000"), 4, BigDecimal.ROUND_HALF_UP));
            }
            if (record.getDailyAvgBalance() != null) {
                record.setDailyAvgBalanceWan(record.getDailyAvgBalance().divide(new BigDecimal("10000"), 4, BigDecimal.ROUND_HALF_UP));
            }
        }

        log.info("[单位转换] 完成，共转换 {} 行", dataList.size());
    }
}