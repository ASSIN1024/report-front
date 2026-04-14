package com.report.pipeline.example;

import com.report.entity.dto.StepContext;
import com.report.pipeline.Pipeline;
import com.report.pipeline.PipelineStep;
import com.report.pipeline.step.DataCleanseStep;
import com.report.pipeline.step.DataAggregateStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class SalesDataPipeline implements Pipeline {

    @Autowired
    private DataCleanseStep dataCleanseStep;

    @Autowired
    private DataAggregateStep dataAggregateStep;

    @Override
    public String getCode() {
        return "sales_data_pipeline";
    }

    @Override
    public String getName() {
        return "销售数据处理流水线";
    }

    @Override
    public List<PipelineStep> getSteps() {
        return Arrays.asList(
            dataCleanseStep,
            dataAggregateStep
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