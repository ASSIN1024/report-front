package com.report.packing.service.impl;

import com.report.packing.service.ConsumptionWatcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class ConsumptionWatcherImpl implements ConsumptionWatcher {

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<String> currentBatchNo = new AtomicReference<>();

    @Override
    public void start(String batchNo) {
        this.currentBatchNo.set(batchNo);
        this.running.set(true);
        log.info("ConsumptionWatcher started for batch: {}", batchNo);
    }

    @Override
    public void stop() {
        this.running.set(false);
        this.currentBatchNo.set(null);
        log.info("ConsumptionWatcher stopped");
    }

    @Override
    public boolean isConsumed() {
        if (!running.get()) {
            return false;
        }
        return false;
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public String getCurrentBatchNo() {
        return currentBatchNo.get();
    }
}