package com.report.packing.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.report.packing.entity.PackingBatch;
import com.report.packing.mapper.PackingBatchMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/packing/batch")
public class PackingBatchController {

    @Autowired
    private PackingBatchMapper batchMapper;

    @GetMapping
    public List<PackingBatch> getBatches(@RequestParam(required = false) String status) {
        if (status == null) {
            return batchMapper.selectList(new QueryWrapper<PackingBatch>().orderByDesc("create_time"));
        }
        return batchMapper.selectList(
            new QueryWrapper<PackingBatch>()
                .eq("status", status)
                .orderByDesc("create_time")
        );
    }

    @GetMapping("/{batchNo}")
    public PackingBatch getBatch(@PathVariable String batchNo) {
        return batchMapper.selectOne(new QueryWrapper<PackingBatch>().eq("batch_no", batchNo));
    }
}