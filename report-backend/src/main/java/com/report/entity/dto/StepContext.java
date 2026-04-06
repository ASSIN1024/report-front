package com.report.entity.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.Map;

@Data
public class StepContext {
    private Long taskId;
    private LocalDate partitionDate;
    private Map<String, Object> params;

    public StepContext(Long taskId, LocalDate partitionDate) {
        this.taskId = taskId;
        this.partitionDate = partitionDate;
    }
}