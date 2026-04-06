package com.report.trigger;

import com.report.pipeline.PipelineExecutor;
import com.report.service.LogService;
import com.report.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class TriggerJob implements Job {

    @Autowired
    private TriggerService triggerService;

    @Autowired
    private TriggerStateManager stateManager;

    @Autowired
    private PipelineExecutor pipelineExecutor;

    @Autowired
    private TaskService taskService;

    @Autowired
    private LogService logService;

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
        TriggerState state = stateManager.getOrCreate(trigger.getTriggerCode());
        LocalDate partitionDate = resolvePartitionDate(trigger.getPartitionPattern());

        int dataCount = triggerService.checkDataExists(trigger, partitionDate);

        if (dataCount > 0) {
            log.info("[{}] 检测到数据: {} 行，分区: {}", trigger.getTriggerName(), dataCount, partitionDate);

            if (!state.isTriggered()) {
                triggerPipeline(trigger, partitionDate);
                stateManager.reset(trigger.getTriggerCode());
            }
        } else {
            state.setRetryCount(state.getRetryCount() + 1);
            state.setLastCheckTime(LocalDateTime.now());

            if (state.getRetryCount() > trigger.getMaxRetries()) {
                log.warn("[{}] 等待数据超时，分区: {}，重试次数: {}",
                    trigger.getTriggerName(), partitionDate, state.getRetryCount());

                markTaskSkipped(trigger, partitionDate, "数据就绪超时");
                stateManager.reset(trigger.getTriggerCode());
            } else {
                log.debug("[{}] 等待数据中，分区: {}，重试: {}/{}",
                    trigger.getTriggerName(), partitionDate,
                    state.getRetryCount(), trigger.getMaxRetries());
            }
        }
    }

    private void triggerPipeline(TriggerConfig trigger, LocalDate partitionDate) {
        try {
            log.info("[{}] 触发Pipeline: {}", trigger.getTriggerName(), trigger.getPipelineCode());
            pipelineExecutor.execute(trigger.getPipelineCode(), partitionDate);
            triggerService.updateLastTriggerTime(trigger.getTriggerCode());
            stateManager.getOrCreate(trigger.getTriggerCode()).setTriggered(true);
        } catch (Exception e) {
            log.error("[{}] Pipeline触发失败: {}", trigger.getTriggerName(), e.getMessage());
        }
    }

    private void markTaskSkipped(TriggerConfig trigger, LocalDate partitionDate, String reason) {
        taskService.createTask(
            "TRIGGER",
            trigger.getTriggerName() + " - " + partitionDate,
            null,
            trigger.getTriggerCode(),
            "SKIPPED: " + reason
        );
    }

    private LocalDate resolvePartitionDate(String pattern) {
        return LocalDate.now();
    }
}