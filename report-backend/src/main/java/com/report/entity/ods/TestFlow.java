package com.report.entity.ods;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("test_flow")
public class TestFlow {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private BigDecimal amount;

    private LocalDate ptDt;
}
