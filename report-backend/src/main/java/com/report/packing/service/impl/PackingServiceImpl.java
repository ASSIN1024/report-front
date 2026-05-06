package com.report.packing.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.report.entity.ProcessedFile;
import com.report.mapper.ProcessedFileMapper;
import com.report.packing.entity.PackingBatch;
import com.report.packing.generator.ConfigTableGenerator;
import com.report.packing.mapper.PackingBatchMapper;
import com.report.packing.service.ConsumptionWatcher;
import com.report.packing.service.PackingConfigService;
import com.report.packing.service.PackingService;
import com.report.util.FtpUtil;
import com.report.util.ZipPackager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class PackingServiceImpl extends ServiceImpl<PackingBatchMapper, PackingBatch> implements PackingService {

    @Autowired
    private PackingConfigService configService;
    @Autowired
    private ProcessedFileMapper processedFileMapper;
    @Autowired
    private ConfigTableGenerator configTableGenerator;
    @Autowired
    private ConsumptionWatcher consumptionWatcher;
    
    // FtpUtil is a static utility class, use FtpUtil.method() directly

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMddHHmmss");

    @Override
    public String pack(List<Long> processedFileIds) {
        if (processedFileIds == null || processedFileIds.isEmpty()) {
            log.info("No files to pack");
            return null;
        }

        String batchNo = "batch_" + SDF.format(new Date()) + "_" + UUID.randomUUID().toString().substring(0, 8);
        log.info("Starting packing for batch: {}", batchNo);

        PackingBatch batch = new PackingBatch();
        batch.setBatchNo(batchNo);
        batch.setStatus(PackingBatch.STATUS_PENDING);
        batch.setFileCount(processedFileIds.size());
        batch.setStartTime(new Date());
        this.save(batch);

        for (Long fileId : processedFileIds) {
            ProcessedFile file = new ProcessedFile();
            file.setId(fileId);
            file.setBatchNo(batchNo);
            processedFileMapper.updateById(file);
        }

        try {
            File configExcel = configTableGenerator.generate(processedFileIds, batchNo);
            File stagingDir = new File(getStagingDir());
            stagingDir.mkdirs();

            String fixedFilename = getFixedFilename();
            File stagingZipFile = new File(stagingDir, fixedFilename);
            File uploadDir = new File(getUploadDir());
            uploadDir.mkdirs();
            File uploadZipFile = new File(uploadDir, fixedFilename);

            List<String> filesToPackage = new java.util.ArrayList<>();
            for (Long fileId : processedFileIds) {
                ProcessedFile pf = processedFileMapper.selectById(fileId);
                if (pf != null && pf.getFilePath() != null) {
                    File f = new File(pf.getFilePath());
                    if (f.exists()) {
                        String originalFileName = pf.getFileName();
                        File tempFile = new File(stagingDir, originalFileName);
                        java.nio.file.Files.copy(f.toPath(), tempFile.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        filesToPackage.add(tempFile.getAbsolutePath());
                    }
                }
            }
            filesToPackage.add(configExcel.getAbsolutePath());

            ZipPackager.packageFiles(stagingZipFile.getAbsolutePath(), filesToPackage);

            for (Long fileId : processedFileIds) {
                ProcessedFile pf = processedFileMapper.selectById(fileId);
                if (pf != null && pf.getFilePath() != null) {
                    File stagingFile = new File(stagingDir, pf.getFileName());
                    if (stagingFile.exists()) {
                        stagingFile.delete();
                    }
                }
            }

            java.nio.file.Files.copy(
                stagingZipFile.toPath(),
                uploadZipFile.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );

            stagingZipFile.delete();
            configExcel.delete();

            batch.setStatus(PackingBatch.STATUS_DONE);
            batch.setEndTime(new Date());
            this.updateById(batch);
            log.info("Batch packed successfully: {} -> outputs.zip", batchNo);
            return batchNo;
        } catch (Exception e) {
            log.error("Failed to pack batch: {}", batchNo, e);
            batch.setStatus("FAILED");
            this.updateById(batch);
            throw new RuntimeException("Packing failed: " + e.getMessage(), e);
        }
    }

    private String getStagingDir() {
        return configService.getStringValue("staging_dir", "/data/ftp-root/staging");
    }

    @Override
    public void upload(String batchNo) {
        log.info("Uploading batch: {}", batchNo);
    }

    @Override
    public boolean canUpload() {
        return !isBeingConsumed();
    }

    @Override
    public boolean isBeingConsumed() {
        String uploadDir = getUploadDir();
        String fixedFilename = getFixedFilename();
        try {
            return false;
        } catch (Exception e) {
            log.error("Failed to check FTP file existence", e);
            return true;
        }
    }

    @Override
    public String getUploadDir() {
        return configService.getStringValue("upload_dir", "/data/ftp-root/for-upload");
    }

    @Override
    public String getDoneDir() {
        return configService.getStringValue("done_dir", "/data/ftp-root/done");
    }

    @Override
    public String getFixedFilename() {
        return configService.getStringValue("fixed_filename", "outputs.zip");
    }

    @Override
    public Long getMaxPackageSize() {
        return configService.getLongValue("max_package_size", 209715200L);
    }
}