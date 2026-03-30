package com.report.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.report.annotation.OperationLogAnnotation;
import com.report.common.result.Result;
import com.report.entity.FtpConfig;
import com.report.service.FtpConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/ftp/config")
public class FtpConfigController {

    @Autowired
    private FtpConfigService ftpConfigService;

    @GetMapping("/page")
    public Result<Page<FtpConfig>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String configName,
            @RequestParam(required = false) Integer status) {
        return Result.success(ftpConfigService.pageList(pageNum, pageSize, configName, status));
    }

    @GetMapping("/list/enabled")
    public Result<List<FtpConfig>> listEnabled() {
        return Result.success(ftpConfigService.listEnabled());
    }

    @GetMapping("/{id}")
    public Result<FtpConfig> getById(@PathVariable Long id) {
        return Result.success(ftpConfigService.getById(id));
    }

    @PostMapping
    @OperationLogAnnotation(module = "FTP配置", operationType = "CREATE", operationDesc = "新增FTP配置")
    public Result<Void> save(@RequestBody FtpConfig ftpConfig) {
        ftpConfigService.save(ftpConfig);
        return Result.success();
    }

    @PutMapping
    @OperationLogAnnotation(module = "FTP配置", operationType = "UPDATE", operationDesc = "修改FTP配置")
    public Result<Void> update(@RequestBody FtpConfig ftpConfig) {
        ftpConfigService.updateById(ftpConfig);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @OperationLogAnnotation(module = "FTP配置", operationType = "DELETE", operationDesc = "删除FTP配置")
    public Result<Void> delete(@PathVariable Long id) {
        ftpConfigService.removeById(id);
        return Result.success();
    }

    @PostMapping("/test/{id}")
    @OperationLogAnnotation(module = "FTP配置", operationType = "TEST", operationDesc = "测试FTP连接")
    public Result<Boolean> testConnection(@PathVariable Long id) {
        return Result.success(ftpConfigService.testConnection(id));
    }
}
