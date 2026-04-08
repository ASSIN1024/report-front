package com.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

@Data
@TableName("table_layer_mapping")
public class TableLayerMapping {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String tableName;

    private String tableLayer;

    private String sourceType;

    private Long sourceId;

    private String sourceName;

    private String businessDomain;

    private String description;

    private String tags;

    @TableLogic
    private Integer marked;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}