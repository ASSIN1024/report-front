package com.report.pipeline.example;

import com.report.entity.dto.StepContext;
import com.report.pipeline.Pipeline;
import com.report.pipeline.PipelineStep;
import com.report.pipeline.step.DepositDwdStep;
import com.report.pipeline.step.DepositDwsStep;
import com.report.pipeline.step.DepositAdsStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.List;

@Component
public class CompanyDepositPipeline implements Pipeline {

    @Autowired
    private DepositDwdStep depositDwdStep;

    @Autowired
    private DepositDwsStep depositDwsStep;

    @Autowired
    private DepositAdsStep depositAdsStep;

    @Override
    public String getCode() {
        return "company_deposit_pipeline";
    }

    @Override
    public String getName() {
        return "公司存款数据处理流水线";
    }

    @Override
    public List<PipelineStep> getSteps() {
        return Arrays.asList(
            depositDwdStep,
            depositDwsStep,
            depositAdsStep
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