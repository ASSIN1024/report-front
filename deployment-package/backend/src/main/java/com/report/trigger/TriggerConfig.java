package com.report.trigger;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("trigger_config")
public class TriggerConfig {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String triggerCode;
    private String triggerName;
    private String sourceTable;
    private String partitionColumn;
    private String partitionPattern;
    private Integer pollIntervalSeconds;
    private Integer maxRetries;
    private String pipelineCode;
    private String status;
    private Date lastTriggerTime;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
