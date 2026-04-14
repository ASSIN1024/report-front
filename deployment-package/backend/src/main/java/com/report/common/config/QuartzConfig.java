package com.report.common.config;

import com.report.job.FtpScanJob;
import com.report.trigger.TriggerJob;
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
    public JobDetail triggerJobDetail() {
        return JobBuilder.newJob(TriggerJob.class)
                .withIdentity("triggerJob")
                .withDescription("数据触发器轮询任务")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger triggerJobTrigger() {
        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInMinutes(1)
                .repeatForever();

        return TriggerBuilder.newTrigger()
                .forJob(triggerJobDetail())
                .withIdentity("triggerJobTrigger")
                .withDescription("数据触发器轮询触发器")
                .withSchedule(scheduleBuilder)
                .startNow()
                .build();
    }
}
