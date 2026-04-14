package com.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("trigger_partition_record")
public class TriggerPartitionRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String triggerCode;

    private Date partitionDate;

    private Boolean triggered;

    private Long pipelineTaskId;

    private Date triggerTime;

    private String status;

    private String instanceId;

    private Date createTime;

    private Date updateTime;

    @Version
    private Integer version;
}
