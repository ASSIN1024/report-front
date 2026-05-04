package com.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.report.entity.ProcessMonitorLog;
import com.report.mapper.ProcessMonitorLogMapper;
import com.report.service.MonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class MonitorServiceImpl extends ServiceImpl<ProcessMonitorLogMapper, ProcessMonitorLog>
        implements MonitorService {

    @Override
    public Long startMonitor(String reportCode, String fileName, String step) {
        ProcessMonitorLog monitorLog = new ProcessMonitorLog();
        monitorLog.setReportCode(reportCode);
        monitorLog.setFileName(fileName);
        monitorLog.setStep(step);
        monitorLog.setStatus(ProcessMonitorLog.STATUS_RUNNING);
        monitorLog.setStartTime(new Date());
        save(monitorLog);

        log.info("[Monitor] started - reportCode={}, fileName={}, step={}, monitorId={}",
                reportCode, fileName, step, monitorLog.getId());
        return monitorLog.getId();
    }

    @Override
    public void updateMonitor(Long monitorId, String step, String status, String message) {
        ProcessMonitorLog monitorLog = getById(monitorId);
        if (monitorLog == null) {
            log.warn("[Monitor] monitorId={} not found", monitorId);
            return;
        }
        monitorLog.setStep(step);
        monitorLog.setStatus(status);
        monitorLog.setMessage(message);
        updateById(monitorLog);

        log.info("[Monitor] updated - monitorId={}, step={}, status={}, message={}",
                monitorId, step, status, message);
    }

    @Override
    public void endMonitor(Long monitorId, String status, String message) {
        ProcessMonitorLog monitorLog = getById(monitorId);
        if (monitorLog == null) {
            log.warn("[Monitor] monitorId={} not found", monitorId);
            return;
        }
        monitorLog.setStatus(status);
        monitorLog.setMessage(message);
        monitorLog.setEndTime(new Date());
        if (monitorLog.getStartTime() != null) {
            monitorLog.setDurationMs(monitorLog.getEndTime().getTime() - monitorLog.getStartTime().getTime());
        }
        updateById(monitorLog);

        log.info("[Monitor] ended - monitorId={}, status={}, message={}, durationMs={}",
                monitorId, status, message, monitorLog.getDurationMs());
    }

    @Override
    public List<ProcessMonitorLog> getMonitorHistory(String reportCode) {
        LambdaQueryWrapper<ProcessMonitorLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProcessMonitorLog::getReportCode, reportCode);
        wrapper.orderByDesc(ProcessMonitorLog::getStartTime);
        return list(wrapper);
    }

    @Override
    public List<ProcessMonitorLog> getFileMonitorHistory(String fileName) {
        LambdaQueryWrapper<ProcessMonitorLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProcessMonitorLog::getFileName, fileName);
        wrapper.orderByDesc(ProcessMonitorLog::getStartTime);
        return list(wrapper);
    }

    @Override
    public Page<ProcessMonitorLog> pageListByReportCode(String reportCode, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<ProcessMonitorLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProcessMonitorLog::getReportCode, reportCode);
        wrapper.orderByDesc(ProcessMonitorLog::getStartTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public ProcessMonitorLog getLatestByReportCode(String reportCode) {
        LambdaQueryWrapper<ProcessMonitorLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProcessMonitorLog::getReportCode, reportCode);
        wrapper.orderByDesc(ProcessMonitorLog::getStartTime);
        wrapper.last("LIMIT 1");
        return getOne(wrapper);
    }
}