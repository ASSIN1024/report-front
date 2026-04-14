package com.report.trigger;

import com.report.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/trigger")
public class TriggerController {

    @Autowired
    @Qualifier("triggerServiceImpl")
    private ITriggerService triggerService;

    @GetMapping
    public Result<List<TriggerConfig>> getAllTriggers() {
        return Result.success(triggerService.getAllEnabled());
    }

    @GetMapping("/{code}")
    public Result<TriggerConfig> getTrigger(@PathVariable String code) {
        TriggerConfig config = triggerService.getByCode(code);
        if (config == null) {
            return Result.fail("触发器不存在");
        }
        return Result.success(config);
    }

    @GetMapping("/state")
    public Result<List<TriggerRealtimeState>> getAllTriggerStates() {
        return Result.success(triggerService.getRealtimeStates());
    }

    @GetMapping("/state/{code}")
    public Result<TriggerRealtimeState> getTriggerState(@PathVariable String code) {
        TriggerRealtimeState state = triggerService.getRealtimeState(code);
        if (state == null) {
            return Result.fail("触发器不存在");
        }
        return Result.success(state);
    }

    @GetMapping("/history/{code}")
    public Result<List<TriggerExecutionLog>> getTriggerHistory(
            @PathVariable String code,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate partitionDate) {
        return Result.success(triggerService.getExecutionHistory(code, partitionDate));
    }

    @PostMapping
    public Result<Void> createTrigger(@RequestBody TriggerConfig config) {
        return Result.success(null);
    }

    @PutMapping("/{code}")
    public Result<Void> updateTrigger(@PathVariable String code, @RequestBody TriggerConfig config) {
        return Result.success(null);
    }

    @DeleteMapping("/{code}")
    public Result<Void> deleteTrigger(@PathVariable String code) {
        return Result.success(null);
    }

    @PostMapping("/{code}/test")
    public Result<Map<String, Object>> testTrigger(@PathVariable String code) {
        TriggerConfig config = triggerService.getByCode(code);
        if (config == null) {
            return Result.fail("触发器不存在");
        }

        Date partitionDate = new Date();
        int dataCount = triggerService.checkDataExists(config, partitionDate);

        Map<String, Object> result = new HashMap<>();
        result.put("triggerCode", code);
        result.put("partitionDate", partitionDate);
        result.put("dataCount", dataCount);
        result.put("hasData", dataCount > 0);

        return Result.success(result);
    }
}
