package com.report.entity.ods;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("ods_company_deposit")
public class OsdCompanyDeposit {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String branchCode;
    private String branchName;
    private String depositType;
    private BigDecimal balance;
    private BigDecimal balanceWan;
    private BigDecimal vsYesterday;
    private BigDecimal vsMonthStart;
    private BigDecimal vsYearStart;
    private BigDecimal dailyAvgBalance;
    private BigDecimal dailyAvgBalanceWan;
    private BigDecimal dailyAvgVsYesterday;
    private BigDecimal dailyAvgVsMonthStart;
    private BigDecimal dailyAvgVsYearStart;
    private BigDecimal monthlyNew;
    private BigDecimal monthlyExpire;
    private BigDecimal yoyGrowthRate;
    private BigDecimal annualTarget;
    private BigDecimal targetCompleteRate;
    private String riskLevel;
    private Date ptDt;
    private Date createTime;
}