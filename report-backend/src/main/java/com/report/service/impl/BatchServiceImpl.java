package com.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.report.entity.BatchRecord;
import com.report.ftp.BuiltInFtpConfig;
import com.report.ftp.BuiltInFtpConfigService;
import com.report.mapper.BatchRecordMapper;
import com.report.service.BatchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Slf4j
@Service
public class BatchServiceImpl extends ServiceImpl<BatchRecordMapper, BatchRecord> implements BatchService {

    @Autowired(required = false)
    private BuiltInFtpConfigService builtInFtpConfigService;

    @Override
    public Page<BatchRecord> pageList(Integer pageNum, Integer pageSize, String status) {
        LambdaQueryWrapper<BatchRecord> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(BatchRecord::getStatus, status);
        }
        wrapper.orderByDesc(BatchRecord::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public void deliverZipIfReady(Long ftpConfigId) {
        log.info("deliverZipIfReady called with ftpConfigId: {} (no-op in simplified version, use built-in FTP)", ftpConfigId);
    }
}
