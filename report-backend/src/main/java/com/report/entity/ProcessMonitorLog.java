package com.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

@Data
@TableName("process_monitor_log")
public class ProcessMonitorLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String reportCode;

    private String fileName;

    private String step;

    private String status;

    private String message;

    private Long durationMs;

    private Date startTime;

    private Date endTime;

    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
}