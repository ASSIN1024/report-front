package com.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.report.entity.AlertRecord;
import com.report.mapper.AlertRecordMapper;
import com.report.service.AlertService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;

@Slf4j
@Service
public class AlertServiceImpl extends ServiceImpl<AlertRecordMapper, AlertRecord> implements AlertService {

    @Override
    public void createAlert(Long reportConfigId, String fileName, String level, String type, String message) {
        AlertRecord alert = new AlertRecord();
        alert.setReportConfigId(reportConfigId);
        alert.setFileName(fileName);
        alert.setAlertLevel(level);
        alert.setAlertType(type);
        alert.setAlertMessage(message);
        alert.setStatus("OPEN");
        alert.setCreateTime(new Date());
        alert.setUpdateTime(new Date());
        save(alert);
        log.warn("Alert created: level={}, type={}, file={}, message={}", level, type, fileName, message);
    }

    @Override
    public Page<AlertRecord> pageList(Integer pageNum, Integer pageSize, String level, String type, String status) {
        LambdaQueryWrapper<AlertRecord> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(level)) {
            wrapper.eq(AlertRecord::getAlertLevel, level);
        }
        if (StringUtils.hasText(type)) {
            wrapper.eq(AlertRecord::getAlertType, type);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(AlertRecord::getStatus, status);
        }
        wrapper.orderByDesc(AlertRecord::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public void resolveAlert(Long alertId, String resolvedBy) {
        AlertRecord alert = getById(alertId);
        if (alert != null) {
            alert.setStatus("RESOLVED");
            alert.setResolvedBy(resolvedBy);
            alert.setResolvedAt(new Date());
            alert.setUpdateTime(new Date());
            updateById(alert);
            log.info("Alert resolved: id={}, resolvedBy={}", alertId, resolvedBy);
        }
    }
}
