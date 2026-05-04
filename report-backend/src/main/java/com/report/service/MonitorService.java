package com.report.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.report.entity.ProcessMonitorLog;
import java.util.List;

public interface MonitorService {

    Long startMonitor(String reportCode, String fileName, String step);

    void updateMonitor(Long monitorId, String step, String status, String message);

    void endMonitor(Long monitorId, String status, String message);

    List<ProcessMonitorLog> getMonitorHistory(String reportCode);

    List<ProcessMonitorLog> getFileMonitorHistory(String fileName);

    Page<ProcessMonitorLog> pageListByReportCode(String reportCode, Integer pageNum, Integer pageSize);

    ProcessMonitorLog getLatestByReportCode(String reportCode);
}