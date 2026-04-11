package com.report.entity.ads;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("ads_company_deposit")
public class AdsCompanyDeposit {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Date reportDate;
    private BigDecimal totalBalance;
    private BigDecimal totalDailyAvg;
    private BigDecimal totalMonthlyNew;
    private BigDecimal totalTarget;
    private BigDecimal completeRate;
    private Integer branchCount;
    private Date ptDt;
    private Date createTime;
}