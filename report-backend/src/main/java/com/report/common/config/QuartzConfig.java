package com.report.common.config;

import com.report.job.BatchPackagingJob;
import com.report.job.FtpScanJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail ftpScanJobDetail() {
        return JobBuilder.newJob(FtpScanJob.class)
                .withIdentity("ftpScanJob")
                .withDescription("FTP扫描任务")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger ftpScanJobTrigger() {
        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInMinutes(5)
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
        return JobBuilder.newJob(BatchPackagingJob.class)
                .withIdentity("batchPackagingJob")
                .withDescription("批量打包任务")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger batchPackagingJobTrigger() {
        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInHours(1)
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
