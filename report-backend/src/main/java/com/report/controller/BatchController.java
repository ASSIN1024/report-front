package com.report.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.report.common.result.Result;
import com.report.entity.BatchRecord;
import com.report.service.BatchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/batch")
public class BatchController {

    @Autowired
    private BatchService batchService;

    @GetMapping
    public Result<Page<BatchRecord>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status) {
        Page<BatchRecord> page = batchService.pageList(pageNum, pageSize, status);
        return Result.success(page);
    }

    @GetMapping("/{id}")
    public Result<BatchRecord> getById(@PathVariable Long id) {
        BatchRecord batch = batchService.getById(id);
        return Result.success(batch);
    }
}
