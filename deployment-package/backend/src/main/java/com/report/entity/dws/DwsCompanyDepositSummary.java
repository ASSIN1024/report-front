package com.report.entity.dws;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("dws_company_deposit_summary")
public class DwsCompanyDepositSummary {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String depositType;
    private BigDecimal totalBalanceWan;
    private BigDecimal totalDailyAvgBalanceWan;
    private BigDecimal totalMonthlyNewWan;
    private BigDecimal totalAnnualTargetWan;
    private BigDecimal completeRate;
    private Integer branchCount;
    private Date ptDt;
    private Date createTime;
}