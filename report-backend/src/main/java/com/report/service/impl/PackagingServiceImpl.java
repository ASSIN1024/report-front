package com.report.service.impl;

import com.report.entity.BatchRecord;
import com.report.ftp.BuiltInFtpConfig;
import com.report.ftp.BuiltInFtpConfigMapper;
import com.report.ftp.BuiltInFtpConfigService;
import com.report.mapper.BatchRecordMapper;
import com.report.service.PackagingService;
import com.report.service.TransformResult;
import com.report.util.ZipPackager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Service
public class PackagingServiceImpl implements PackagingService {

    @Autowired(required = false)
    private BuiltInFtpConfigService builtInFtpConfigService;

    @Autowired(required = false)
    private BuiltInFtpConfigMapper builtInFtpConfigMapper;

    @Autowired
    private BatchRecordMapper batchRecordMapper;

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMddHHmmss");

    @Override
    public void moveToStagingDir(String standardExcelPath, String sourceFileName, TransformResult result) {
        try {
            File standardFile = new File(standardExcelPath);
            if (!standardFile.exists()) {
                log.warn("Standard file not found: {}", standardExcelPath);
                return;
            }

            String stagingDir = System.getProperty("java.io.tmpdir") + File.separator + "staging-temp";
            new File(stagingDir).mkdirs();

            String destFileName = sourceFileName.replace(".xlsx", "_standard.xlsx");
            File destFile = new File(stagingDir, destFileName);

            try (FileInputStream fis = new FileInputStream(standardFile);
                 FileOutputStream fos = new FileOutputStream(destFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }

            String metaFileName = stagingDir + File.separator + destFileName + ".meta";
            try (FileOutputStream fos = new FileOutputStream(metaFileName)) {
                Properties props = new Properties();
                props.setProperty("source_file", sourceFileName);
                props.setProperty("db_name", result.getDbName() != null ? result.getDbName() : "");
                props.setProperty("table_name", result.getTableName() != null ? result.getTableName() : "");
                props.setProperty("field_mapping", result.getFieldMappingJson() != null ? result.getFieldMappingJson() : "");
                props.setProperty("pt_dt", result.getPtDt() != null ? result.getPtDt() : "");
                props.setProperty("load_mode", result.getLoadMode() != null ? result.getLoadMode() : "partitioned-append");
                props.setProperty("staged_at", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                props.store(fos, "Staging metadata");
            }

            log.info("Moved to staging: {} -> {}", sourceFileName, destFile.getAbsolutePath());

        } catch (Exception e) {
            log.error("Failed to move file to staging: {}", sourceFileName, e);
            throw new RuntimeException("Failed to move file to staging: " + e.getMessage(), e);
        }
    }

    @Override
    public void collectAndPackageAll() {
        log.info("collectAndPackageAll called (no-op in simplified version)");
    }

    @Override
    public int collectAndPackageBySizeLimit(long maxSizeBytes) {
        log.info("collectAndPackageBySizeLimit called with maxSizeBytes: {} (no-op in simplified version)", maxSizeBytes);
        return 0;
    }

    @Override
    public String packageToStaging(Long ftpConfigId, String standardExcelPath, String sourceFileName, TransformResult result) {
        BuiltInFtpConfig ftpConfig = builtInFtpConfigMapper != null ? builtInFtpConfigMapper.getConfig() : null;
        String stagingDir;
        if (ftpConfig != null) {
            stagingDir = ftpConfig.getRootDirectory() + "/staging";
        } else {
            stagingDir = System.getProperty("java.io.tmpdir") + File.separator + "staging";
        }

        String batchCode = "batch_" + SDF.format(new Date()) + "_" + UUID.randomUUID().toString().substring(0, 8);
        String zipFileName = batchCode + ".zip";

        String localTempDir = System.getProperty("java.io.tmpdir") + File.separator + "packaging";
        new File(localTempDir).mkdirs();

        String localZipPath = localTempDir + File.separator + zipFileName;

        try {
            List<String> filesToPackage = new ArrayList<>();
            filesToPackage.add(standardExcelPath);

            ZipPackager.packageFiles(localZipPath, filesToPackage);

            File zipFile = new File(localZipPath);
            long zipSize = zipFile.length();

            Path targetDir = new File(stagingDir).toPath();
            Files.createDirectories(targetDir);
            Path targetPath = targetDir.resolve(zipFileName);
            Files.copy(zipFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            BatchRecord batch = new BatchRecord();
            batch.setBatchCode(batchCode);
            batch.setFtpConfigId(ftpConfigId != null ? ftpConfigId : -1L);
            batch.setZipFileName(zipFileName);
            batch.setFileCount(1);
            batch.setTotalSize(zipSize);
            batch.setStatus("CREATED");
            batch.setCreateTime(new Date());
            batch.setUpdateTime(new Date());
            batchRecordMapper.insert(batch);

            zipFile.delete();

            log.info("Package created: {} with size={}", zipFileName, zipSize);
            return batchCode;

        } catch (Exception e) {
            log.error("Failed to package files", e);
            throw new RuntimeException("Failed to package files: " + e.getMessage(), e);
        }
    }
}
