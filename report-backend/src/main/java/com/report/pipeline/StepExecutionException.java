package com.report.pipeline;

public class StepExecutionException extends Exception {
    private final String stepName;

    public StepExecutionException(String stepName, String message) {
        super(message);
        this.stepName = stepName;
    }

    public StepExecutionException(String stepName, String message, Throwable cause) {
        super(message, cause);
        this.stepName = stepName;
    }

    public String getStepName() {
        return stepName;
    }
}