package com.report.pipeline;

import com.report.entity.dto.StepContext;
import java.util.List;

public interface Pipeline {
    String getCode();
    String getName();
    List<PipelineStep> getSteps();
    boolean isIdempotent();
    void execute(StepContext context) throws Exception;
}