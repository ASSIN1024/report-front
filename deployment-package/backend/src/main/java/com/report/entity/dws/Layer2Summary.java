package com.report.entity.dws;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("layer_2_summary")
public class Layer2Summary {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String productName;

    private BigDecimal totalAmount;

    private Integer orderCount;

    private Date ptDt;

    private Date createTime;
}
