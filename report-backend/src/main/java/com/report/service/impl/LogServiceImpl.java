package com.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.report.entity.TaskExecutionLog;
import com.report.mapper.TaskExecutionLogMapper;
import com.report.service.LogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class LogServiceImpl extends ServiceImpl<TaskExecutionLogMapper, TaskExecutionLog> implements LogService {

    @Override
    public Page<TaskExecutionLog> pageList(Long taskExecutionId, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<TaskExecutionLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskExecutionLog::getTaskExecutionId, taskExecutionId);
        wrapper.orderByDesc(TaskExecutionLog::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public void logInfo(Long taskExecutionId, String message) {
        saveLog(taskExecutionId, "INFO", message);
        log.info("[Task-{}] {}", taskExecutionId, message);
    }

    @Override
    public void logWarn(Long taskExecutionId, String message) {
        saveLog(taskExecutionId, "WARN", message);
        log.warn("[Task-{}] {}", taskExecutionId, message);
    }

    @Override
    public void logError(Long taskExecutionId, String message) {
        saveLog(taskExecutionId, "ERROR", message);
        log.error("[Task-{}] {}", taskExecutionId, message);
    }

    @Override
    public List<TaskExecutionLog> listByTaskExecutionId(Long taskExecutionId) {
        LambdaQueryWrapper<TaskExecutionLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskExecutionLog::getTaskExecutionId, taskExecutionId);
        wrapper.orderByAsc(TaskExecutionLog::getCreateTime);
        return list(wrapper);
    }

    private void saveLog(Long taskExecutionId, String level, String message) {
        TaskExecutionLog logEntity = new TaskExecutionLog();
        logEntity.setTaskExecutionId(taskExecutionId);
        logEntity.setLogLevel(level);
        logEntity.setLogMessage(message);
        logEntity.setCreateTime(new Date());
        save(logEntity);
    }
}
