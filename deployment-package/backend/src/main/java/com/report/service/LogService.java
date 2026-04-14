package com.report.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.report.entity.TaskExecutionLog;

import java.util.List;

public interface LogService extends IService<TaskExecutionLog> {

    Page<TaskExecutionLog> pageList(Long taskExecutionId, Integer pageNum, Integer pageSize);

    void logInfo(Long taskExecutionId, String message);

    void logWarn(Long taskExecutionId, String message);

    void logError(Long taskExecutionId, String message);

    List<TaskExecutionLog> listByTaskExecutionId(Long taskExecutionId);

    void saveLog(Long taskExecutionId, String logLevel, String logMessage);
}
