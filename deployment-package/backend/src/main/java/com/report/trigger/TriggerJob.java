package com.report.trigger;

import com.report.pipeline.PipelineExecutor;
import com.report.service.LogService;
import com.report.service.PartitionRecordService;
import com.report.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Slf4j
@Component
@DisallowConcurrentExecution
public class TriggerJob implements Job {

    @Autowired
    @Qualifier("triggerServiceImpl")
    private ITriggerService triggerService;

    @Autowired
    @Qualifier("databaseTriggerStateManager")
    private TriggerStateManager stateManager;

    @Autowired
    private PipelineExecutor pipelineExecutor;

    @Autowired
    private TaskService taskService;

    @Autowired
    private LogService logService;

    @Autowired
    private PartitionRecordService partitionRecordService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("TriggerJob 开始执行");

        List<TriggerConfig> triggers = triggerService.getAllEnabled();
        for (TriggerConfig trigger : triggers) {
            processTrigger(trigger);
        }

        log.info("TriggerJob 执行完成");
    }

    private void processTrigger(TriggerConfig trigger) {
        Date partitionDate = new Date();

        if (partitionRecordService.isPartitionTriggered(trigger.getTriggerCode(), partitionDate)) {
            log.debug("[{}] 分区 {} 已触发，跳过", trigger.getTriggerName(), partitionDate);
            return;
        }

        TriggerState state = stateManager.getOrCreate(trigger.getTriggerCode());

        log.debug("[{}] 开始检查数据，轮询间隔: {}秒",
            trigger.getTriggerName(),
            trigger.getPollIntervalSeconds());

        int dataCount = triggerService.checkDataExists(trigger, partitionDate);

        if (dataCount > 0) {
            log.info("[{}] 检测到数据: {} 行，分区: {}", trigger.getTriggerName(), dataCount, partitionDate);

            if (partitionRecordService.markPartitionTriggering(trigger.getTriggerCode(), partitionDate)) {
                triggerPipeline(trigger, partitionDate, dataCount);
            } else {
                log.debug("[{}] 分区 {} 已被其他实例标记，跳过", trigger.getTriggerName(), partitionDate);
            }
        } else {
            stateManager.incrementRetryCount(trigger.getTriggerCode());
            state.setLastCheckTime(new Date());

            if (state.getRetryCount() > trigger.getMaxRetries()) {
                log.warn("[{}] 等待数据超时，分区: {}，重试次数: {}",
                    trigger.getTriggerName(), partitionDate, state.getRetryCount());

                logTriggerExecution(trigger, partitionDate, 0, "SKIPPED", null, state.getRetryCount());
                markTaskSkipped(trigger, partitionDate, "数据就绪超时");
                stateManager.reset(trigger.getTriggerCode());
            } else {
                log.debug("[{}] 等待数据中，分区: {}，重试: {}/{}",
                    trigger.getTriggerName(), partitionDate,
                    state.getRetryCount(), trigger.getMaxRetries());

                logTriggerExecution(trigger, partitionDate, 0, "WAITING", null, state.getRetryCount());
            }
        }
    }

    private void triggerPipeline(TriggerConfig trigger, Date partitionDate, int dataCount) {
        Long pipelineTaskId = null;
        String status = "FAILED";
        int retryCount = 0;

        try {
            log.info("[{}] 触发Pipeline: {}", trigger.getTriggerName(), trigger.getPipelineCode());
            java.time.LocalDate localDate = new java.sql.Date(partitionDate.getTime()).toLocalDate();
            pipelineTaskId = pipelineExecutor.execute(trigger.getPipelineCode(), localDate);
            triggerService.updateLastTriggerTime(trigger.getTriggerCode());
            partitionRecordService.markPartitionTriggered(trigger.getTriggerCode(), partitionDate, pipelineTaskId);
            status = "TRIGGERED";
        } catch (Exception e) {
            log.error("[{}] Pipeline触发失败: {}", trigger.getTriggerName(), e.getMessage());
            status = "FAILED";
        }

        TriggerState state = stateManager.getOrCreate(trigger.getTriggerCode());
        retryCount = state.getRetryCount();
        logTriggerExecution(trigger, partitionDate, dataCount, status, pipelineTaskId, retryCount);
    }

    private void logTriggerExecution(TriggerConfig trigger, Date partitionDate, int dataCount, String status, Long pipelineTaskId, int retryCount) {
        try {
            TriggerExecutionLog executionLog = new TriggerExecutionLog();
            executionLog.setTriggerCode(trigger.getTriggerCode());
            executionLog.setTriggerName(trigger.getTriggerName());
            executionLog.setPartitionDate(partitionDate);
            executionLog.setDataCount(dataCount);
            executionLog.setTriggerStatus(status);
            executionLog.setPipelineTaskId(pipelineTaskId);
            executionLog.setRetryCount(retryCount);
            executionLog.setExecutionTime(new Date());

            triggerService.logTriggerExecution(executionLog);
        } catch (Exception e) {
            log.error("[{}] 记录执行日志失败: {}", trigger.getTriggerName(), e.getMessage());
        }
    }

    private void markTaskSkipped(TriggerConfig trigger, Date partitionDate, String reason) {
        taskService.createTask(
            "TRIGGER",
            trigger.getTriggerName() + " - " + partitionDate,
            null,
            trigger.getTriggerCode(),
            "SKIPPED: " + reason
        );
    }
}
