package com.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.report.entity.BatchRecord;
import com.report.entity.FtpConfig;
import com.report.mapper.BatchRecordMapper;
import com.report.mapper.FtpConfigMapper;
import com.report.service.BatchService;
import com.report.util.FtpUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

@Slf4j
@Service
public class BatchServiceImpl extends ServiceImpl<BatchRecordMapper, BatchRecord> implements BatchService {

    @Autowired
    private FtpConfigMapper ftpConfigMapper;

    @Override
    public Page<BatchRecord> pageList(Integer pageNum, Integer pageSize, String status) {
        LambdaQueryWrapper<BatchRecord> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(BatchRecord::getStatus, status);
        }
        wrapper.orderByDesc(BatchRecord::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public void deliverZipIfReady(Long ftpConfigId) {
        FtpConfig ftpConfig = ftpConfigMapper.selectById(ftpConfigId);
        if (ftpConfig == null) {
            log.warn("FTP config not found: {}", ftpConfigId);
            return;
        }

        String forUploadDir = resolveDir(ftpConfig.getForUploadDir(), ftpConfig.getScanPath(), "for-upload");
        String stagingDir = resolveDir(ftpConfig.getStagingDir(), ftpConfig.getScanPath(), "staging");

        FTPClient ftpClient = null;
        try {
            ftpClient = FtpUtil.connect(ftpConfig);
            if (!isDirectoryEmpty(ftpClient, forUploadDir)) {
                log.debug("for-upload directory not empty, waiting for RPA to consume");
                return;
            }

            String earliestFile = getEarliestFile(ftpClient, stagingDir);
            if (earliestFile == null) {
                log.debug("No files in staging directory");
                return;
            }

            boolean renamed = ftpClient.rename(stagingDir + "/" + earliestFile, forUploadDir + "/output.zip");
            if (renamed) {
                log.info("Delivered ZIP: {} -> {}/output.zip", earliestFile, forUploadDir);
                markBatchDelivered(earliestFile);
            } else {
                log.error("Failed to rename ZIP from staging to for-upload");
            }
        } catch (IOException e) {
            log.error("FTP operation failed during deliverZipIfReady", e);
        } finally {
            FtpUtil.disconnect(ftpClient);
        }
    }

    private String resolveDir(String configuredDir, String scanPath, String defaultSuffix) {
        if (StringUtils.hasText(configuredDir)) {
            return configuredDir;
        }
        if (StringUtils.hasText(scanPath)) {
            return scanPath + "/" + defaultSuffix;
        }
        return "/" + defaultSuffix;
    }

    private boolean isDirectoryEmpty(FTPClient ftpClient, String dirPath) throws IOException {
        String[] files = ftpClient.listNames(dirPath);
        return files == null || files.length == 0;
    }

    private String getEarliestFile(FTPClient ftpClient, String dirPath) throws IOException {
        String[] files = ftpClient.listNames(dirPath);
        if (files == null || files.length == 0) {
            return null;
        }
        Arrays.sort(files);
        String earliest = files[0];
        int lastSlash = earliest.lastIndexOf('/');
        return lastSlash >= 0 ? earliest.substring(lastSlash + 1) : earliest;
    }

    private void markBatchDelivered(String zipFileName) {
        LambdaQueryWrapper<BatchRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BatchRecord::getZipFileName, zipFileName);
        wrapper.eq(BatchRecord::getStatus, "CREATED");
        wrapper.last("LIMIT 1");
        BatchRecord batch = getOne(wrapper);
        if (batch != null) {
            batch.setStatus("DELIVERED");
            batch.setDeliveredAt(new Date());
            batch.setUpdateTime(new Date());
            updateById(batch);
        }
    }
}
