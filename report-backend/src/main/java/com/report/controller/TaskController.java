package com.report.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.report.common.result.Result;
import com.report.entity.TaskExecution;
import com.report.entity.dto.TaskQueryDTO;
import com.report.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/task")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @GetMapping("/page")
    public Result<Page<TaskExecution>> page(TaskQueryDTO queryDTO) {
        return Result.success(taskService.pageList(queryDTO));
    }

    @GetMapping("/{id}")
    public Result<TaskExecution> getById(@PathVariable Long id) {
        return Result.success(taskService.getById(id));
    }

    @PostMapping("/retry/{id}")
    public Result<Void> retry(@PathVariable Long id) {
        TaskExecution task = taskService.getById(id);
        if (task == null) {
            return Result.error("任务不存在");
        }
        taskService.updateTaskStatus(id, "PENDING");
        return Result.success();
    }

    @PostMapping("/cancel/{id}")
    public Result<Void> cancel(@PathVariable Long id) {
        taskService.updateTaskStatus(id, "CANCELLED");
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        taskService.removeById(id);
        return Result.success();
    }
}
