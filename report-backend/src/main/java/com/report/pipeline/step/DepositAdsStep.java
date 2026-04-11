package com.report.pipeline.step;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.report.entity.dwd.DwdCompanyDeposit;
import com.report.entity.ads.AdsCompanyDeposit;
import com.report.entity.dto.StepContext;
import com.report.mapper.DwdCompanyDepositMapper;
import com.report.mapper.AdsCompanyDepositMapper;
import com.report.pipeline.MyBatisPlusAbstractStep;
import com.report.pipeline.StepExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class DepositAdsStep extends MyBatisPlusAbstractStep {

    @Autowired
    private DwdCompanyDepositMapper dwdCompanyDepositMapper;

    @Autowired
    private AdsCompanyDepositMapper adsCompanyDepositMapper;

    @Override
    public String getStepName() {
        return "ADS表生成";
    }

    @Override
    protected String getTableName() {
        return "ads_company_deposit";
    }

    @Override
    protected void doExecute(StepContext context) throws StepExecutionException {
        Date partitionDate = Date.from(context.getPartitionDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
        log.info("[ADS生成] 读取 DWD 数据，分区: {}", context.getPartitionDate());

        List<DwdCompanyDeposit> dwdData = dwdCompanyDepositMapper.selectList(
            new LambdaQueryWrapper<DwdCompanyDeposit>()
                .eq(DwdCompanyDeposit::getPtDt, partitionDate)
        );

        log.info("[ADS生成] 读取到 {} 行数据，生成 ADS 汇总表", dwdData.size());

        AdsCompanyDeposit ads = new AdsCompanyDeposit();
        ads.setReportDate(partitionDate);
        ads.setBranchCount(dwdData.size());

        BigDecimal totalBalance = dwdData.stream()
            .map(DwdCompanyDeposit::getBalance)
            .filter(b -> b != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        ads.setTotalBalance(totalBalance);

        BigDecimal totalDailyAvg = dwdData.stream()
            .map(DwdCompanyDeposit::getDailyAvgBalance)
            .filter(b -> b != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        ads.setTotalDailyAvg(totalDailyAvg);

        BigDecimal totalMonthlyNew = dwdData.stream()
            .map(DwdCompanyDeposit::getMonthlyNew)
            .filter(b -> b != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        ads.setTotalMonthlyNew(totalMonthlyNew);

        BigDecimal totalTarget = dwdData.stream()
            .map(DwdCompanyDeposit::getAnnualTarget)
            .filter(b -> b != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        ads.setTotalTarget(totalTarget);

        if (totalTarget.compareTo(BigDecimal.ZERO) > 0) {
            ads.setCompleteRate(totalBalance.divide(totalTarget, 6, RoundingMode.HALF_UP));
        } else {
            ads.setCompleteRate(BigDecimal.ZERO);
        }

        ads.setPtDt(partitionDate);
        adsCompanyDepositMapper.insert(ads);

        log.info("[ADS生成] 完成，生成 ADS 记录 1 行");
    }
}