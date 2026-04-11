package com.report.trigger;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.Date;

@Data
@TableName("trigger_execution_log")
public class TriggerExecutionLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String triggerCode;

    private String triggerName;

    private Date partitionDate;

    private Integer dataCount;

    private String triggerStatus;

    private Long pipelineTaskId;

    private String errorMessage;

    private Integer retryCount;

    private Date executionTime;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}