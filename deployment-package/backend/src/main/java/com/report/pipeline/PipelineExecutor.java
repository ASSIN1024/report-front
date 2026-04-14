package com.report.pipeline;

import com.report.entity.TaskExecution;
import com.report.entity.dto.StepContext;
import com.report.service.LogService;
import com.report.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class PipelineExecutor {

    @Autowired
    private TaskService taskService;

    @Autowired
    private LogService logService;

    @Autowired
    private List<Pipeline> pipelineList;

    public Long execute(String pipelineCode, LocalDate partitionDate) {
        Pipeline pipeline = pipelineList.stream()
            .filter(p -> p.getCode().equals(pipelineCode))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Pipeline不存在: " + pipelineCode));

        TaskExecution task = taskService.createTask(
            "PIPELINE",
            pipeline.getName() + " - " + partitionDate,
            null,
            pipelineCode,
            null
        );

        StepContext context = new StepContext(task.getId(), partitionDate);
        context.setParams(new HashMap<>());

        try {
            taskService.updateTaskStatus(task.getId(), "RUNNING");
            logService.saveLog(task.getId(), "INFO", "Pipeline开始执行: " + pipelineCode);

            pipeline.execute(context);

            taskService.finishTask(task.getId(), "SUCCESS", null);
            logService.saveLog(task.getId(), "INFO", "Pipeline执行完成: " + pipelineCode);
        } catch (Exception e) {
            log.error("Pipeline执行失败: {}", pipelineCode, e);
            taskService.finishTask(task.getId(), "FAILED", e.getMessage());
            logService.saveLog(task.getId(), "ERROR", "Pipeline执行失败: " + e.getMessage());
        }

        return task.getId();
    }
}