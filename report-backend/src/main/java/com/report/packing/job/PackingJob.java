package com.report.packing.job;

import com.report.packing.manager.PackingManager;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@DisallowConcurrentExecution
public class PackingJob {

    @Autowired
    private PackingManager packingManager;

    public void execute() {
        log.info("PackingJob triggered");
        try {
            packingManager.executePacking();
        } catch (Exception e) {
            log.error("PackingJob execution failed", e);
        }
    }
}