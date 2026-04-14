package com.report.entity.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class TaskQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String taskType;

    private String taskName;

    private String status;

    private Date startTimeBegin;

    private Date startTimeEnd;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
