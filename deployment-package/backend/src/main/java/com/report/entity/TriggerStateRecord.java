package com.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

@Data
@TableName("trigger_state_record")
public class TriggerStateRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String triggerCode;

    private Integer retryCount;

    private Date lastCheckTime;

    private Boolean triggered;

    private String instanceId;

    private Date createTime;

    private Date updateTime;

    @Version
    private Integer version;
}
