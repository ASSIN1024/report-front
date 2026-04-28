package com.report.packing.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.report.packing.entity.AlertRecord;
import com.report.packing.mapper.AlertRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/packing/alerts")
public class PackingAlertController {

    @Autowired
    private AlertRecordMapper alertRecordMapper;

    @GetMapping
    public List<AlertRecord> getAlerts(@RequestParam(required = false) String status) {
        if (status == null) {
            return alertRecordMapper.selectList(null);
        }
        return alertRecordMapper.selectList(
            new QueryWrapper<AlertRecord>().eq("status", status)
        );
    }

    @GetMapping("/count")
    public long getAlertCount(@RequestParam(required = false) String status) {
        if (status == null) {
            return alertRecordMapper.selectCount(null);
        }
        return alertRecordMapper.selectCount(new QueryWrapper<AlertRecord>().eq("status", status));
    }

    @PutMapping("/{id}/resolve")
    public void resolveAlert(@PathVariable Long id) {
        AlertRecord alert = alertRecordMapper.selectById(id);
        if (alert != null) {
            alert.setStatus(AlertRecord.STATUS_RESOLVED);
            alert.setResolveTime(new Date());
            alertRecordMapper.updateById(alert);
        }
    }

    @PutMapping("/{id}/ignore")
    public void ignoreAlert(@PathVariable Long id) {
        AlertRecord alert = alertRecordMapper.selectById(id);
        if (alert != null) {
            alert.setStatus(AlertRecord.STATUS_IGNORED);
            alert.setResolveTime(new Date());
            alertRecordMapper.updateById(alert);
        }
    }
}