package com.report.pipeline.example;

import com.report.entity.dto.StepContext;
import com.report.pipeline.Pipeline;
import com.report.pipeline.PipelineStep;
import com.report.pipeline.step.DataCleanseStep;
import com.report.pipeline.step.testFlow.TestFlowAggregateStep;
import com.report.pipeline.step.testFlow.TestFlowCleanseStep;
import com.report.pipeline.step.DataAggregateStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class TestFlowPipeline implements Pipeline {

    @Autowired
    private TestFlowCleanseStep testFlowCleanseStep;

    @Autowired
    private TestFlowAggregateStep ttestFlowAggregateStep;

    @Override
    public String getCode() {
        return "test_flow_pipeline";
    }

    @Override
    public String getName() {
        return "测试流水线";
    }

    @Override
    public List<PipelineStep> getSteps() {
        return Arrays.asList(
            testFlowCleanseStep,
            ttestFlowAggregateStep
        );
    }

    @Override
    public boolean isIdempotent() {
        return true;
    }

    @Override
    public void execute(StepContext context) throws Exception {
        for (PipelineStep step : getSteps()) {
            step.execute(context);
        }
    }
}