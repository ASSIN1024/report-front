package com.report.entity.dwd;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("dwd_clean_tets_flow")
public class DwdCleanTestFlow {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private BigDecimal amount;

    private Date ptDt;

    private Date createTime;
}
