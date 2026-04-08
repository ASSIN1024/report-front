package com.report.entity.dws;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("dwd_tets_flow_agg")
public class DwdTestFlowAgg {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private BigDecimal totalAmount;

    private LocalDate ptDt;
}
