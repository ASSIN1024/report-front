package com.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.report.entity.BatchRecord;
import com.report.ftp.BuiltInFtpConfig;
import com.report.ftp.FtpConfigProvider;
import com.report.mapper.BatchRecordMapper;
import com.report.service.BatchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class BatchServiceImpl extends ServiceImpl<BatchRecordMapper, BatchRecord> implements BatchService {

    @Autowired(required = false)
    private FtpConfigProvider ftpConfigProvider;

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
        BuiltInFtpConfig ftpConfig = ftpConfigProvider != null ? ftpConfigProvider.getConfig() : null;
        if (ftpConfig == null) {
            log.warn("FTP config not found, cannot deliver zip");
            return;
        }

        String stagingDir = ftpConfig.getRootDirectory() + File.separator + "staging";
        File dir = new File(stagingDir);
        if (!dir.exists() || !dir.isDirectory()) {
            log.info("Staging directory not found: {}", stagingDir);
            return;
        }

        File[] zipFiles = dir.listFiles((d, name) -> name.endsWith(".zip"));
        if (zipFiles == null || zipFiles.length == 0) {
            log.info("No zip files to deliver");
            return;
        }

        String deliveryDir = ftpConfig.getRootDirectory() + File.separator + "delivery";
        new File(deliveryDir).mkdirs();

        for (File zipFile : zipFiles) {
            try {
                File destFile = new File(deliveryDir, zipFile.getName());
                java.nio.file.Files.copy(zipFile.toPath(), destFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                BatchRecord batchRecord = findBatchByZipName(zipFile.getName());
                if (batchRecord != null) {
                    batchRecord.setStatus("DELIVERED");
                    batchRecord.setDeliveredAt(new Date());
                    batchRecord.setUpdateTime(new Date());
                    updateById(batchRecord);
                }

                zipFile.delete();
                log.info("Delivered zip: {} -> {}", zipFile.getName(), destFile.getAbsolutePath());

            } catch (Exception e) {
                log.error("Failed to deliver zip: {}", zipFile.getName(), e);
            }
        }
    }

    private BatchRecord findBatchByZipName(String zipFileName) {
        LambdaQueryWrapper<BatchRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BatchRecord::getZipFileName, zipFileName);
        wrapper.last("LIMIT 1");
        return getOne(wrapper);
    }
}
