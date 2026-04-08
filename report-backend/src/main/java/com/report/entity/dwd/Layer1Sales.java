package com.report.entity.dwd;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("layer_1_sales")
public class Layer1Sales {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String productName;

    private BigDecimal amount;

    private LocalDate ptDt;
}
