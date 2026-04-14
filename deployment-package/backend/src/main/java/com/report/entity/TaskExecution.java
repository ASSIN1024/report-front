package com.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("task_execution")
public class TaskExecution implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String taskType;

    private String taskName;

    private Long reportConfigId;

    private String fileName;

    private String filePath;

    private String status;

    private Integer totalRows;

    private Integer successRows;

    private Integer failedRows;

    private String errorMessage;

    private Date startTime;

    private Date endTime;

    private Long duration;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
