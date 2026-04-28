package com.report.job;

import com.report.service.PackagingService;
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
public class BatchPackagingJob implements Job {

    @Autowired
    private PackagingService packagingService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("BatchPackagingJob started");

        try {
            packagingService.collectAndPackageAll();
            log.info("BatchPackagingJob completed");

            context.setResult("Batch packaging completed");
        } catch (Exception e) {
            log.error("BatchPackagingJob failed", e);
            throw new JobExecutionException(e);
        }
    }
}