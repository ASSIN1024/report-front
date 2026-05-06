package com.report.packing.manager.impl;

import com.report.common.config.JobProperties;
import com.report.entity.ProcessedFile;
import com.report.mapper.ProcessedFileMapper;
import com.report.packing.entity.PackingBatch;
import com.report.packing.manager.PackingManager;
import com.report.packing.service.ConsumptionWatcher;
import com.report.packing.service.PackingConfigService;
import com.report.packing.service.PackingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
public class PackingManagerImpl implements PackingManager {

    @Autowired
    private PackingService packingService;
    @Autowired
    private PackingConfigService configService;
    @Autowired
    private ConsumptionWatcher consumptionWatcher;
    @Autowired
    private ProcessedFileMapper processedFileMapper;
    @Autowired
    private JobProperties jobProperties;

    @Override
    public void executePacking() {
        log.info("PackingManager executing...");

        // 检查消费状态
        if (!packingService.canUpload()) {
            log.info("Previous package is being consumed, waiting...");
            boolean consumed = checkAndWaitForConsumption();
            if (!consumed) {
                log.warn("Consumption timeout, will retry next cycle");
                return;
            }
        }

        // 获取待打包文件
        List<ProcessedFile> pendingFiles = processedFileMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ProcessedFile>()
                .eq("status", "PROCESSED")
                .isNull("batch_no")
                .orderByAsc("create_time")  // 按创建时间顺序打包
        );

        if (pendingFiles.isEmpty()) {
            log.info("No pending files to pack");
            return;
        }

        // 计算预估大小
        long estimatedSize = pendingFiles.stream()
            .mapToLong(f -> f.getFileSize() != null ? f.getFileSize() : 0)
            .sum();

        long maxPackageSize = jobProperties.getBatchPackaging().getMaxPackageSize(); // 200MB

        if (estimatedSize > maxPackageSize) {
            // 超过阈值，分批打包
            List<Long> fileIds = new java.util.ArrayList<>();
            long currentSize = 0;

            for (ProcessedFile file : pendingFiles) {
                long fileSize = file.getFileSize() != null ? file.getFileSize() : 0;
                if (currentSize + fileSize > maxPackageSize && !fileIds.isEmpty()) {
                    // 达到阈值，打包当前批次
                    packingService.pack(fileIds);
                    log.info("分批打包完成，已打包 {} 个文件，大小 {} bytes，剩余文件下一轮处理",
                        fileIds.size(), currentSize);
                    return;  // 本轮结束，等待消费后下一轮继续
                }
                fileIds.add(file.getId());
                currentSize += fileSize;
            }

            // 打包剩余文件（未超过阈值）
            if (!fileIds.isEmpty()) {
                packingService.pack(fileIds);
            }
        } else {
            // 未超过阈值，打包全部
            List<Long> fileIds = pendingFiles.stream()
                .map(ProcessedFile::getId)
                .collect(Collectors.toList());
            packingService.pack(fileIds);
        }

        log.info("PackingManager execution completed");
    }

    @Override
    public boolean checkAndWaitForConsumption() {
        int pollingInterval = configService.getIntValue("polling_interval", 30);
        int maxWaitMinutes = 60;
        int maxChecks = (maxWaitMinutes * 60) / pollingInterval;

        for (int i = 0; i < maxChecks; i++) {
            if (!packingService.isBeingConsumed()) {
                return true;
            }
            try {
                TimeUnit.SECONDS.sleep(pollingInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    @Override
    public void triggerNextPacking() {
        log.info("Triggering next packing");
        executePacking();
    }
}