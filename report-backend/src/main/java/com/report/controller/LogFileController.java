package com.report.controller;

import com.report.common.result.Result;
import com.report.dto.LogQueryDTO;
import com.report.dto.LogResultDTO;
import com.report.service.LogFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/log/file")
public class LogFileController {

    @Autowired
    private LogFileService logFileService;

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> listLogFiles() {
        return Result.success(logFileService.listLogFiles());
    }

    @GetMapping("/info")
    public Result<Map<String, Object>> getFileInfo(@RequestParam String logType) {
        return Result.success(logFileService.getFileInfo(logType));
    }

    @GetMapping("/query")
    public Result<LogResultDTO> queryLogs(
            @RequestParam(required = false) String logType,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "100") Integer pageSize) {
        
        LogQueryDTO query = new LogQueryDTO();
        query.setLogType(logType);
        query.setLevel(level);
        query.setKeyword(keyword);
        query.setStartDate(startDate);
        query.setEndDate(endDate);
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        
        return Result.success(logFileService.queryLogs(query));
    }

    @GetMapping("/recent")
    public Result<LogResultDTO> getRecentLogs(
            @RequestParam(defaultValue = "all") String logType,
            @RequestParam(defaultValue = "100") Integer lines) {
        
        LogResultDTO result = new LogResultDTO();
        result.setLogType(logType);
        result.setLines(logFileService.getRecentLogs(logType, lines));
        result.setTotal(result.getLines().size());
        
        return Result.success(result);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportLog(
            @RequestParam String logType,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String keyword) {
        
        LogQueryDTO query = new LogQueryDTO();
        query.setLogType(logType);
        query.setLevel(level);
        query.setKeyword(keyword);
        
        byte[] content = logFileService.exportLog(query);
        
        String fileName = logType + "-" + System.currentTimeMillis() + ".log";
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"" + URLEncoder.encode(fileName, StandardCharsets.UTF_8) + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(content);
    }
}
