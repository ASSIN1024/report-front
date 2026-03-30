package com.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("operation_log")
public class OperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String module;

    private String operationType;

    private String operationDesc;

    private String targetId;

    private String targetName;

    private String beforeData;

    private String afterData;

    private Integer result;

    private String errorMsg;

    private String operatorIp;

    private String operatorName;

    private Long duration;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
