package com.report.pipeline.step;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.report.entity.ods.OsdCompanyDeposit;
import com.report.entity.dwd.DwdCompanyDeposit;
import com.report.entity.dto.StepContext;
import com.report.mapper.OsdCompanyDepositMapper;
import com.report.mapper.DwdCompanyDepositMapper;
import com.report.pipeline.MyBatisPlusAbstractStep;
import com.report.pipeline.StepExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DepositDwdStep extends MyBatisPlusAbstractStep {

    @Autowired
    private OsdCompanyDepositMapper osdCompanyDepositMapper;

    @Autowired
    private DwdCompanyDepositMapper dwdCompanyDepositMapper;

    @Override
    public String getStepName() {
        return "DWD表生成";
    }

    @Override
    protected String getTableName() {
        return "dwd_company_deposit";
    }

    @Override
    protected void doExecute(StepContext context) throws StepExecutionException {
        Date partitionDate = Date.from(context.getPartitionDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
        log.info("[DWD生成] 读取 OSD 数据，分区: {}", context.getPartitionDate());

        List<OsdCompanyDeposit> osdData = osdCompanyDepositMapper.selectList(
            new LambdaQueryWrapper<OsdCompanyDeposit>()
                .eq(OsdCompanyDeposit::getPtDt, partitionDate)
                .ne(OsdCompanyDeposit::getBranchName, "合  计")
        );

        log.info("[DWD生成] 读取到 {} 行原始数据", osdData.size());

        List<DwdCompanyDeposit> dwdData = osdData.stream()
            .filter(row -> row.getBranchName() != null && !row.getBranchName().contains("合计"))
            .map(this::convertToDwd)
            .collect(Collectors.toList());

        for (DwdCompanyDeposit record : dwdData) {
            dwdCompanyDepositMapper.insert(record);
        }

        log.info("[DWD生成] 完成，写入 {} 行到 dwd_company_deposit", dwdData.size());
    }

    private DwdCompanyDeposit convertToDwd(OsdCompanyDeposit osd) {
        DwdCompanyDeposit dwd = new DwdCompanyDeposit();
        dwd.setBranchCode(osd.getBranchCode());
        dwd.setBranchName(osd.getBranchName());
        dwd.setDepositType(osd.getDepositType());
        dwd.setBalance(osd.getBalanceWan() != null ? osd.getBalanceWan() : BigDecimal.ZERO);
        dwd.setDailyAvgBalance(osd.getDailyAvgBalanceWan() != null ? osd.getDailyAvgBalanceWan() : BigDecimal.ZERO);
        dwd.setMonthlyNew(osd.getMonthlyNew() != null ? osd.getMonthlyNew().divide(new BigDecimal("10000"), 4, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO);
        dwd.setMonthlyExpire(osd.getMonthlyExpire() != null ? osd.getMonthlyExpire().divide(new BigDecimal("10000"), 4, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO);
        dwd.setAnnualTarget(osd.getAnnualTarget() != null ? osd.getAnnualTarget().divide(new BigDecimal("10000"), 4, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO);
        dwd.setRiskLevel(osd.getRiskLevel());
        dwd.setPtDt(osd.getPtDt());
        return dwd;
    }
}