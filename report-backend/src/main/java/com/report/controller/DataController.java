package com.report.controller;

import com.report.common.result.Result;
import com.report.entity.dto.DataQueryDTO;
import com.report.service.DataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/data")
public class DataController {

    @Autowired
    private DataService dataService;

    @GetMapping("/tables")
    public Result<List<String>> getOutputTables() {
        return Result.success(dataService.getOutputTables());
    }

    @GetMapping("/columns")
    public Result<List<String>> getTableColumns(@RequestParam String tableName) {
        return Result.success(dataService.getTableColumns(tableName));
    }

    @PostMapping("/query")
    public Result<Map<String, Object>> queryData(@RequestBody DataQueryDTO queryDTO) {
        return Result.success(dataService.queryPage(queryDTO));
    }

    @PostMapping("/list")
    public Result<List<Map<String, Object>>> queryList(@RequestBody DataQueryDTO queryDTO) {
        return Result.success(dataService.queryList(queryDTO));
    }
}
