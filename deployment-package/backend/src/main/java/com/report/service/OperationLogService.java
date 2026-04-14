package com.report.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.report.entity.OperationLog;

public interface OperationLogService extends IService<OperationLog> {

    Page<OperationLog> pageList(Integer pageNum, Integer pageSize, String module, String operationType, Integer result);

    void log(String module, String operationType, String operationDesc, String targetId, String targetName,
             String beforeData, String afterData, Integer result, String errorMsg, Long duration);
}
