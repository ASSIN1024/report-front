package com.report.pipeline.step;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.report.entity.dwd.DwdCompanyDeposit;
import com.report.entity.dws.DwsCompanyDepositSummary;
import com.report.entity.dto.StepContext;
import com.report.mapper.DwdCompanyDepositMapper;
import com.report.mapper.DwsCompanyDepositSummaryMapper;
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
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DepositDwsStep extends MyBatisPlusAbstractStep {

    @Autowired
    private DwdCompanyDepositMapper dwdCompanyDepositMapper;

    @Autowired
    private DwsCompanyDepositSummaryMapper dwsCompanyDepositSummaryMapper;

    @Override
    public String getStepName() {
        return "DWS表生成";
    }

    @Override
    protected String getTableName() {
        return "dws_company_deposit_summary";
    }

    @Override
    protected void doExecute(StepContext context) throws StepExecutionException {
        Date partitionDate = Date.from(context.getPartitionDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
        log.info("[DWS生成] 读取 DWD 数据，分区: {}", context.getPartitionDate());

        List<DwdCompanyDeposit> dwdData = dwdCompanyDepositMapper.selectList(
            new LambdaQueryWrapper<DwdCompanyDeposit>()
                .eq(DwdCompanyDeposit::getPtDt, partitionDate)
        );

        log.info("[DWS生成] 读取到 {} 行数据，开始按存款类型汇总", dwdData.size());

        Map<String, List<DwdCompanyDeposit>> groupedByType = dwdData.stream()
            .collect(Collectors.groupingBy(DwdCompanyDeposit::getDepositType));

        for (Map.Entry<String, List<DwdCompanyDeposit>> entry : groupedByType.entrySet()) {
            List<DwdCompanyDeposit> typeData = entry.getValue();
            DwsCompanyDepositSummary summary = new DwsCompanyDepositSummary();
            summary.setDepositType(entry.getKey());
            summary.setBranchCount(typeData.size());

            BigDecimal totalBalance = typeData.stream()
                .map(DwdCompanyDeposit::getBalance)
                .filter(b -> b != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            summary.setTotalBalanceWan(totalBalance);

            BigDecimal totalDailyAvg = typeData.stream()
                .map(DwdCompanyDeposit::getDailyAvgBalance)
                .filter(b -> b != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            summary.setTotalDailyAvgBalanceWan(totalDailyAvg);

            BigDecimal totalMonthlyNew = typeData.stream()
                .map(DwdCompanyDeposit::getMonthlyNew)
                .filter(b -> b != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            summary.setTotalMonthlyNewWan(totalMonthlyNew);

            BigDecimal totalTarget = typeData.stream()
                .map(DwdCompanyDeposit::getAnnualTarget)
                .filter(b -> b != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            summary.setTotalAnnualTargetWan(totalTarget);

            if (totalTarget.compareTo(BigDecimal.ZERO) > 0) {
                summary.setCompleteRate(totalBalance.divide(totalTarget, 6, RoundingMode.HALF_UP));
            } else {
                summary.setCompleteRate(BigDecimal.ZERO);
            }

            summary.setPtDt(partitionDate);
            dwsCompanyDepositSummaryMapper.insert(summary);
        }

        log.info("[DWS生成] 完成，生成 {} 个存款类型的汇总数据", groupedByType.size());
    }
}