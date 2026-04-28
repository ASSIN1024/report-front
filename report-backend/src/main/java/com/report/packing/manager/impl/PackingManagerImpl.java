package com.report.packing.manager.impl;

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

    @Override
    public void executePacking() {
        log.info("PackingManager executing...");

        if (!packingService.canUpload()) {
            log.info("Previous package is being consumed, waiting...");
            boolean consumed = checkAndWaitForConsumption();
            if (!consumed) {
                log.warn("Consumption timeout, will retry next cycle");
                return;
            }
        }

        List<ProcessedFile> pendingFiles = processedFileMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ProcessedFile>()
                .eq("status", "PROCESSED")
                .isNull("batch_no")
        );

        if (pendingFiles.isEmpty()) {
            log.info("No pending files to pack");
            return;
        }

        Long maxSize = packingService.getMaxPackageSize();
        List<Long> fileIds = new java.util.ArrayList<>();
        long currentSize = 0;

        for (ProcessedFile file : pendingFiles) {
            long fileSize = file.getFileSize() != null ? file.getFileSize() : 0;
            if (currentSize + fileSize > maxSize && !fileIds.isEmpty()) {
                packingService.pack(fileIds);
                fileIds.clear();
                currentSize = 0;
            }
            fileIds.add(file.getId());
            currentSize += fileSize;
        }

        if (!fileIds.isEmpty()) {
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