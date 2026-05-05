package com.report.common.config;

import com.report.job.BatchPackagingJob;
import com.report.job.FtpScanJob;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Autowired
    private JobProperties jobProperties;

    @Bean
    public JobDetail ftpScanJobDetail() {
        if (!jobProperties.getFtpScan().isEnabled()) {
            return null;
        }
        return JobBuilder.newJob(FtpScanJob.class)
                .withIdentity("ftpScanJob")
                .withDescription("FTP扫描任务")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger ftpScanJobTrigger() {
        if (!jobProperties.getFtpScan().isEnabled()) {
            return null;
        }
        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInMinutes(jobProperties.getFtpScan().getIntervalMinutes())
                .repeatForever();

        return TriggerBuilder.newTrigger()
                .forJob(ftpScanJobDetail())
                .withIdentity("ftpScanTrigger")
                .withDescription("FTP扫描触发器")
                .withSchedule(scheduleBuilder)
                .startNow()
                .build();
    }

    @Bean
    public JobDetail batchPackagingJobDetail() {
        if (!jobProperties.getBatchPackaging().isEnabled()) {
            return null;
        }
        return JobBuilder.newJob(BatchPackagingJob.class)
                .withIdentity("batchPackagingJob")
                .withDescription("批量打包任务")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger batchPackagingJobTrigger() {
        if (!jobProperties.getBatchPackaging().isEnabled()) {
            return null;
        }
        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInMinutes(jobProperties.getBatchPackaging().getIntervalMinutes())
                .repeatForever();

        return TriggerBuilder.newTrigger()
                .forJob(batchPackagingJobDetail())
                .withIdentity("batchPackagingTrigger")
                .withDescription("批量打包触发器")
                .withSchedule(scheduleBuilder)
                .startNow()
                .build();
    }
}
