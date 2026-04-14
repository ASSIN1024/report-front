package com.report.ftp.controller;

import com.report.annotation.OperationLogAnnotation;
import com.report.common.result.Result;
import com.report.ftp.BuiltInFtpConfig;
import com.report.ftp.BuiltInFtpConfigMapper;
import com.report.ftp.EmbeddedFtpServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/built-in-ftp")
public class BuiltInFtpConfigController {

    @Autowired
    private BuiltInFtpConfigMapper builtInFtpConfigMapper;

    @Autowired
    private EmbeddedFtpServer embeddedFtpServer;

    @GetMapping("/config")
    public Result<BuiltInFtpConfig> getConfig() {
        return Result.success(builtInFtpConfigMapper.getConfig());
    }

    @PutMapping("/config")
    @OperationLogAnnotation(module = "内置FTP", operationType = "UPDATE", operationDesc = "修改内置FTP配置")
    public Result<Void> updateConfig(@RequestBody BuiltInFtpConfig config) {
        if (embeddedFtpServer.isRunning()) {
            return Result.fail("FTP服务正在运行，请先停止服务");
        }
        config.setId(1L);
        return Result.success();
    }

    @PostMapping("/start")
    @OperationLogAnnotation(module = "内置FTP", operationType = "START", operationDesc = "启动内置FTP服务")
    public Result<Map<String, Object>> start() {
        Map<String, Object> result = new HashMap<>();
        boolean success = embeddedFtpServer.start();
        result.put("running", success);
        if (success) {
            BuiltInFtpConfig config = builtInFtpConfigMapper.getConfig();
            result.put("port", config.getPort());
            result.put("connectedClients", embeddedFtpServer.getConnectedClients());
            return Result.success(result);
        } else {
            return Result.fail("启动FTP服务失败");
        }
    }

    @PostMapping("/stop")
    @OperationLogAnnotation(module = "内置FTP", operationType = "STOP", operationDesc = "停止内置FTP服务")
    public Result<Void> stop() {
        embeddedFtpServer.stop();
        return Result.success();
    }

    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        Map<String, Object> result = new HashMap<>();
        result.put("running", embeddedFtpServer.isRunning());
        if (embeddedFtpServer.isRunning()) {
            BuiltInFtpConfig config = builtInFtpConfigMapper.getConfig();
            result.put("port", config.getPort());
            result.put("connectedClients", embeddedFtpServer.getConnectedClients());
        }
        return Result.success(result);
    }
}