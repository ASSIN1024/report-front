package com.report.controller;

import com.report.common.result.Result;
import com.report.entity.TableLayerMapping;
import com.report.service.DataCenterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/data-center")
public class DataCenterController {

    @Autowired
    private DataCenterService dataCenterService;

    @GetMapping("/tables")
    public Result<List<TableLayerMapping>> listTables(
            @RequestParam(required = false) String tableLayer,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String businessDomain) {
        return Result.success(dataCenterService.listTables(tableLayer, sourceType, businessDomain));
    }

    @GetMapping("/tables/{tableName}")
    public Result<TableLayerMapping> getTable(@PathVariable String tableName) {
        return Result.success(dataCenterService.getTableByName(tableName));
    }

    @PutMapping("/tables")
    public Result<Void> updateTable(@RequestBody TableLayerMapping mapping) {
        dataCenterService.updateTableMapping(mapping);
        return Result.success();
    }

    @GetMapping("/untagged")
    public Result<List<TableLayerMapping>> listUntagged() {
        return Result.success(dataCenterService.listUntaggedTables());
    }

    @PostMapping("/scan")
    public Result<List<String>> scanTables() {
        return Result.success(dataCenterService.scanNewTables());
    }

    @GetMapping("/tables/{tableName}/columns")
    public Result<List<Map<String, Object>>> getColumns(@PathVariable String tableName) {
        return Result.success(dataCenterService.getTableColumns(tableName));
    }

    @GetMapping("/tables/{tableName}/data")
    public Result<Map<String, Object>> getData(
            @PathVariable String tableName,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String condition) {
        return Result.success(dataCenterService.getTableData(tableName, pageNum, pageSize, condition));
    }
}