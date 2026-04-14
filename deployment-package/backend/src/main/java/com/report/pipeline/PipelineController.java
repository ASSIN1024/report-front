package com.report.pipeline;

import com.report.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/pipeline")
public class PipelineController {

    @Autowired
    private PipelineExecutor pipelineExecutor;

    @Autowired
    private List<Pipeline> pipelineList;

    @GetMapping
    public Result<Map<String, Object>> getAllPipelines() {
        Map<String, Object> result = new HashMap<>();
        result.put("pipelines", pipelineList.stream().map(Pipeline::getCode).toArray(String[]::new));
        return Result.success(result);
    }

    @PostMapping("/{code}/execute")
    public Result<Map<String, Object>> executePipeline(
            @PathVariable String code,
            @RequestParam(required = false) String partitionDate) {

        log.info("手动触发 Pipeline: {}, partitionDate: {}", code, partitionDate);

        LocalDate ptDate = partitionDate != null
            ? LocalDate.parse(partitionDate)
            : LocalDate.now();

        try {
            Long taskId = pipelineExecutor.execute(code, ptDate);

            Map<String, Object> result = new HashMap<>();
            result.put("taskId", taskId);
            result.put("pipelineCode", code);
            result.put("partitionDate", ptDate);
            result.put("status", "EXECUTED");

            return Result.success(result);
        } catch (Exception e) {
            log.error("Pipeline执行失败: {}", code, e);
            return Result.fail("Pipeline执行失败: " + e.getMessage());
        }
    }
}
