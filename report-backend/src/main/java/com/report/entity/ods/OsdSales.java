package com.report.entity.ods;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("osd_sales")
public class OsdSales {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String productName;

    private BigDecimal amount;

    private LocalDate ptDt;
}
