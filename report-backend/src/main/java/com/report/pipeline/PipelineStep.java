package com.report.pipeline;

import com.report.entity.dto.StepContext;

public interface PipelineStep {
    String getStepName();
    void execute(StepContext context) throws StepExecutionException;
    boolean isOverwrite();
    String getTargetTable();
}