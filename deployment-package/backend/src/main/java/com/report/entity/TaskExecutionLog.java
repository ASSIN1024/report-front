package com.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("task_execution_log")
public class TaskExecutionLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long taskExecutionId;

    private String logLevel;

    private String logMessage;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
