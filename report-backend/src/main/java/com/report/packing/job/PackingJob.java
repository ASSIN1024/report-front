package com.report.packing.job;

import com.report.packing.manager.PackingManager;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@DisallowConcurrentExecution
public class PackingJob implements Job {

    @Autowired
    private PackingManager packingManager;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("PackingJob triggered");
        try {
            packingManager.executePacking();
        } catch (Exception e) {
            log.error("PackingJob execution failed", e);
            throw new JobExecutionException(e);
        }
    }
}