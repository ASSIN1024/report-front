package com.report.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.report.common.result.Result;
import com.report.entity.SystemConfig;
import com.report.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system/config")
public class SystemConfigController {

    @Autowired
    private SystemConfigService systemConfigService;

    @GetMapping("/page")
    public Result<Page<SystemConfig>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(systemConfigService.page(new Page<>(pageNum, pageSize)));
    }

    @GetMapping
    public Result<String> getValue(@RequestParam String configKey) {
        return Result.success(systemConfigService.getConfigValue(configKey));
    }

    @PostMapping
    public Result<Void> setValue(@RequestParam String configKey, @RequestParam String configValue) {
        systemConfigService.setConfigValue(configKey, configValue);
        return Result.success();
    }
}
