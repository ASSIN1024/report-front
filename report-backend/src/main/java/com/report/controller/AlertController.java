package com.report.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.report.common.result.Result;
import com.report.entity.AlertRecord;
import com.report.service.AlertService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/alert")
public class AlertController {

    @Autowired
    private AlertService alertService;

    @GetMapping
    public Result<Page<AlertRecord>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        Page<AlertRecord> page = alertService.pageList(pageNum, pageSize, level, type, status);
        return Result.success(page);
    }

    @GetMapping("/{id}")
    public Result<AlertRecord> getById(@PathVariable Long id) {
        AlertRecord alert = alertService.getById(id);
        return Result.success(alert);
    }

    @PutMapping("/{id}/resolve")
    public Result<Void> resolve(@PathVariable Long id) {
        alertService.resolveAlert(id, "admin");
        return Result.success(null);
    }
}
