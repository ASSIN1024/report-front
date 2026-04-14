package com.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.report.entity.OperationLog;
import com.report.mapper.OperationLogMapper;
import com.report.service.OperationLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {

    @Override
    public Page<OperationLog> pageList(Integer pageNum, Integer pageSize, String module, String operationType, Integer result) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(module)) {
            wrapper.like(OperationLog::getModule, module);
        }
        if (StringUtils.hasText(operationType)) {
            wrapper.eq(OperationLog::getOperationType, operationType);
        }
        if (result != null) {
            wrapper.eq(OperationLog::getResult, result);
        }
        wrapper.orderByDesc(OperationLog::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Async
    @Override
    public void log(String module, String operationType, String operationDesc, String targetId, String targetName,
                    String beforeData, String afterData, Integer result, String errorMsg, Long duration) {
        try {
            OperationLog operationLog = new OperationLog();
            operationLog.setModule(module);
            operationLog.setOperationType(operationType);
            operationLog.setOperationDesc(operationDesc);
            operationLog.setTargetId(targetId);
            operationLog.setTargetName(targetName);
            operationLog.setBeforeData(beforeData);
            operationLog.setAfterData(afterData);
            operationLog.setResult(result);
            operationLog.setErrorMsg(errorMsg);
            operationLog.setDuration(duration);
            save(operationLog);
            log.info("操作日志记录成功: {} - {}", module, operationDesc);
        } catch (Exception e) {
            log.error("操作日志记录失败: {}", e.getMessage(), e);
        }
    }
}
