package com.report.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.report.entity.TaskExecution;
import com.report.entity.dto.TaskQueryDTO;

public interface TaskService extends IService<TaskExecution> {

    Page<TaskExecution> pageList(TaskQueryDTO queryDTO);

    TaskExecution createTask(String taskType, String taskName, Long reportConfigId, String fileName, String filePath);

    void updateTaskStatus(Long taskId, String status);

    void updateTaskProgress(Long taskId, Integer totalRows, Integer successRows, Integer failedRows);

    void finishTask(Long taskId, String status, String errorMessage);
}
