package com.report.packing.service;

public interface ConsumptionWatcher {
    void start(String batchNo);
    void stop();
    boolean isConsumed();
    boolean isRunning();
    String getCurrentBatchNo();
}