package com.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.report.entity.TaskExecution;
import com.report.entity.dto.TaskQueryDTO;
import com.report.mapper.TaskExecutionMapper;
import com.report.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;

@Slf4j
@Service
public class TaskServiceImpl extends ServiceImpl<TaskExecutionMapper, TaskExecution> implements TaskService {

    @Override
    public Page<TaskExecution> pageList(TaskQueryDTO queryDTO) {
        LambdaQueryWrapper<TaskExecution> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(queryDTO.getTaskType())) {
            wrapper.eq(TaskExecution::getTaskType, queryDTO.getTaskType());
        }
        if (StringUtils.hasText(queryDTO.getTaskName())) {
            wrapper.like(TaskExecution::getTaskName, queryDTO.getTaskName());
        }
        if (StringUtils.hasText(queryDTO.getStatus())) {
            wrapper.eq(TaskExecution::getStatus, queryDTO.getStatus());
        }
        if (queryDTO.getStartTimeBegin() != null) {
            wrapper.ge(TaskExecution::getStartTime, queryDTO.getStartTimeBegin());
        }
        if (queryDTO.getStartTimeEnd() != null) {
            wrapper.le(TaskExecution::getStartTime, queryDTO.getStartTimeEnd());
        }
        wrapper.orderByDesc(TaskExecution::getCreateTime);
        return page(new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize()), wrapper);
    }

    @Override
    public TaskExecution createTask(String taskType, String taskName, Long reportConfigId, String fileName, String filePath) {
        TaskExecution task = new TaskExecution();
        task.setTaskType(taskType);
        task.setTaskName(taskName);
        task.setReportConfigId(reportConfigId);
        task.setFileName(fileName);
        task.setFilePath(filePath);
        task.setStatus("PENDING");
        task.setTotalRows(0);
        task.setSuccessRows(0);
        task.setFailedRows(0);
        task.setCreateTime(new Date());
        save(task);
        return task;
    }

    @Override
    public void updateTaskStatus(Long taskId, String status) {
        TaskExecution task = new TaskExecution();
        task.setId(taskId);
        task.setStatus(status);
        if ("RUNNING".equals(status)) {
            task.setStartTime(new Date());
        }
        updateById(task);
    }

    @Override
    public void updateTaskProgress(Long taskId, Integer totalRows, Integer successRows, Integer failedRows) {
        TaskExecution task = new TaskExecution();
        task.setId(taskId);
        task.setTotalRows(totalRows);
        task.setSuccessRows(successRows);
        task.setFailedRows(failedRows);
        updateById(task);
    }

    @Override
    public void finishTask(Long taskId, String status, String errorMessage) {
        TaskExecution task = new TaskExecution();
        task.setId(taskId);
        task.setStatus(status);
        task.setEndTime(new Date());
        task.setErrorMessage(errorMessage);
        TaskExecution existing = getById(taskId);
        if (existing != null && existing.getStartTime() != null) {
            long duration = System.currentTimeMillis() - existing.getStartTime().getTime();
            task.setDuration(duration);
        }
        updateById(task);
    }
}
