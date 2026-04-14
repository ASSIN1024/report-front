package com.report.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.report.common.result.Result;
import com.report.entity.TaskExecutionLog;
import com.report.service.LogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/log")
public class LogController {

    @Autowired
    private LogService logService;

    @GetMapping("/page")
    public Result<Page<TaskExecutionLog>> page(
            @RequestParam Long taskExecutionId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(logService.pageList(taskExecutionId, pageNum, pageSize));
    }

    @GetMapping("/list/{taskExecutionId}")
    public Result<List<TaskExecutionLog>> listByTaskExecutionId(@PathVariable Long taskExecutionId) {
        return Result.success(logService.listByTaskExecutionId(taskExecutionId));
    }
}
