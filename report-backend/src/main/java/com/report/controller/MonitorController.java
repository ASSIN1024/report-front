package com.report.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.report.common.result.Result;
import com.report.entity.ProcessMonitorLog;
import com.report.service.MonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/monitor")
public class MonitorController {

    @Autowired
    private MonitorService monitorService;

    @GetMapping("/report/{reportCode}")
    public Result<Page<ProcessMonitorLog>> getReportMonitorHistory(
            @PathVariable String reportCode,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<ProcessMonitorLog> page = monitorService.pageListByReportCode(reportCode, pageNum, pageSize);
        return Result.success(page);
    }

    @GetMapping("/file/{fileName}")
    public Result<List<ProcessMonitorLog>> getFileMonitorHistory(@PathVariable String fileName) {
        List<ProcessMonitorLog> logs = monitorService.getFileMonitorHistory(fileName);
        return Result.success(logs);
    }

    @GetMapping("/realtime/{reportCode}")
    public Result<Map<String, Object>> getRealtimeProgress(@PathVariable String reportCode) {
        ProcessMonitorLog latest = monitorService.getLatestByReportCode(reportCode);
        Map<String, Object> result = new HashMap<>();
        result.put("latest", latest);
        result.put("history", monitorService.getMonitorHistory(reportCode));
        return Result.success(result);
    }
}