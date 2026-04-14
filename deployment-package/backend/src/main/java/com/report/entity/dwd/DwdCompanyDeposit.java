package com.report.entity.dwd;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("dwd_company_deposit")
public class DwdCompanyDeposit {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String branchCode;
    private String branchName;
    private String depositType;
    private BigDecimal balance;
    private BigDecimal dailyAvgBalance;
    private BigDecimal monthlyNew;
    private BigDecimal monthlyExpire;
    private BigDecimal annualTarget;
    private String riskLevel;
    private Date ptDt;
    private Date createTime;
}