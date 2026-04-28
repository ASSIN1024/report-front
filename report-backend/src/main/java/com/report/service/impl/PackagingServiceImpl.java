package com.report.service.impl;

import com.report.entity.BatchRecord;
import com.report.entity.FtpConfig;
import com.report.service.TransformResult;
import com.report.mapper.BatchRecordMapper;
import com.report.mapper.FtpConfigMapper;
import com.report.service.PackagingService;
import com.report.util.ConfigExcelWriter;
import com.report.util.FtpUtil;
import com.report.util.ZipPackager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Service
public class PackagingServiceImpl implements PackagingService {

    @Autowired
    private FtpConfigMapper ftpConfigMapper;

    @Autowired
    private BatchRecordMapper batchRecordMapper;

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMddHHmmss");
    private static final long DEFAULT_MAX_ZIP_SIZE = 200 * 1024 * 1024L;

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
        collectAndPackageBySizeLimit(DEFAULT_MAX_ZIP_SIZE);
    }

    @Override
    public int collectAndPackageBySizeLimit(long maxSizeBytes) {
        String stagingDir = System.getProperty("java.io.tmpdir") + File.separator + "staging-temp";
        File stagingFolder = new File(stagingDir);

        if (!stagingFolder.exists() || !stagingFolder.isDirectory()) {
            log.info("Staging directory not exists or is empty");
            return 0;
        }

        File[] files = stagingFolder.listFiles((dir, name) -> name.endsWith("_standard.xlsx"));
        if (files == null || files.length == 0) {
            log.info("No files in staging to package");
            return 0;
        }

        List<File> pendingFiles = new ArrayList<>(Arrays.asList(files));
        int packageCount = 0;
        List<File> currentBatch = new ArrayList<>();
        long currentBatchSize = 0;

        for (File file : pendingFiles) {
            long fileSize = file.length();
            if (currentBatchSize + fileSize > maxSizeBytes && !currentBatch.isEmpty()) {
                packageBatchFiles(currentBatch, maxSizeBytes);
                packageCount++;
                currentBatch.clear();
                currentBatchSize = 0;
            }
            currentBatch.add(file);
            currentBatchSize += fileSize;
        }

        if (!currentBatch.isEmpty()) {
            packageBatchFiles(currentBatch, maxSizeBytes);
            packageCount++;
        }

        log.info("Created {} batch packages from {} files", packageCount, pendingFiles.size());
        return packageCount;
    }

    private void packageBatchFiles(List<File> files, long maxSizeBytes) {
        if (files.isEmpty()) {
            return;
        }

        String batchCode = "batch_" + SDF.format(new Date()) + "_" + UUID.randomUUID().toString().substring(0, 8);
        String zipFileName = batchCode + ".zip";

        List<Map<String, Object>> configRecords = new ArrayList<>();

        for (File file : files) {
            Map<String, String> metadata = extractMetadata(file);
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("source_file", metadata.get("source_file"));
            record.put("table_type", "hive");
            record.put("db_name", metadata.get("db_name"));
            record.put("table_name", metadata.get("table_name"));
            record.put("is_overseas", 0);
            record.put("field_mapping", metadata.get("field_mapping"));
            record.put("load_mode", metadata.get("load_mode"));
            String ptDt = metadata.get("pt_dt");
            record.put("partition_info", ptDt != null && !ptDt.isEmpty() ? "pt_dt='" + ptDt + "'" : "pt_dt='2022-01-01'");
            record.put("executor_num", 4);
            record.put("executor_cores", 4);
            record.put("executor_memory", "8G");
            record.put("driver_num", 2);
            record.put("driver_memory", "2G");
            configRecords.add(record);
        }

        String localTempDir = System.getProperty("java.io.tmpdir") + File.separator + "packaging";
        new File(localTempDir).mkdirs();

        List<String> filesToPackage = new ArrayList<>();
        for (File file : files) {
            filesToPackage.add(file.getAbsolutePath());

            String metaFile = file.getAbsolutePath() + ".meta";
            if (new File(metaFile).exists()) {
                filesToPackage.add(metaFile);
            }
        }

        String localConfigExcel = localTempDir + File.separator + "config_" + batchCode + ".xlsx";
        try {
            ConfigExcelWriter.write(localConfigExcel, configRecords);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write config Excel", e);
        }
        filesToPackage.add(localConfigExcel);

        String localZipPath = localTempDir + File.separator + zipFileName;

        try {
            ZipPackager.packageFiles(localZipPath, filesToPackage);

            File zipFile = new File(localZipPath);
            long zipSize = zipFile.length();

            List<FtpConfig> ftpConfigs = ftpConfigMapper.selectList(null);
            if (!ftpConfigs.isEmpty()) {
                FtpConfig ftpConfig = ftpConfigs.get(0);
                String stagingDir = resolveDir(ftpConfig.getStagingDir(), ftpConfig.getScanPath(), "staging");
                uploadToFtp(ftpConfig, localZipPath, stagingDir + "/" + zipFileName);
            }

            BatchRecord batch = new BatchRecord();
            batch.setBatchCode(batchCode);
            batch.setFtpConfigId(1L);
            batch.setZipFileName(zipFileName);
            batch.setFileCount(files.size());
            batch.setTotalSize(zipSize);
            batch.setStatus("CREATED");
            batch.setCreateTime(new Date());
            batch.setUpdateTime(new Date());
            batchRecordMapper.insert(batch);

            for (File file : files) {
                file.delete();
                new File(file.getAbsolutePath() + ".meta").delete();
            }
            new File(localConfigExcel).delete();
            zipFile.delete();

            log.info("Package created: {} with {} files, size={}", zipFileName, files.size(), zipSize);

        } catch (Exception e) {
            log.error("Failed to package batch files", e);
            throw new RuntimeException("Failed to package batch files: " + e.getMessage(), e);
        }
    }

    private Map<String, String> extractMetadata(File standardFile) {
        Map<String, String> metadata = new HashMap<>();
        String metaFile = standardFile.getAbsolutePath() + ".meta";
        try {
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(metaFile)) {
                props.load(fis);
                metadata.put("source_file", props.getProperty("source_file", ""));
                metadata.put("db_name", props.getProperty("db_name", ""));
                metadata.put("table_name", props.getProperty("table_name", ""));
                metadata.put("field_mapping", props.getProperty("field_mapping", ""));
                metadata.put("pt_dt", props.getProperty("pt_dt", ""));
                metadata.put("load_mode", props.getProperty("load_mode", "partitioned-append"));
                metadata.put("staged_at", props.getProperty("staged_at", ""));
            }
        } catch (Exception e) {
            log.warn("Failed to read metadata for: {}", standardFile.getName(), e);
        }
        return metadata;
    }

    private String getSourceFileName(File standardFile) {
        Map<String, String> metadata = extractMetadata(standardFile);
        String sourceFile = metadata.get("source_file");
        if (sourceFile != null && !sourceFile.isEmpty()) {
            return sourceFile;
        }
        String name = standardFile.getName();
        return name.replace("_standard.xlsx", ".xlsx");
    }

    @Override
    public String packageToStaging(Long ftpConfigId, String standardExcelPath, String sourceFileName, TransformResult result) {
        FtpConfig ftpConfig = ftpConfigMapper.selectById(ftpConfigId);
        String stagingDir = resolveDir(ftpConfig.getStagingDir(), ftpConfig.getScanPath(), "staging");

        String batchCode = "batch_" + SDF.format(new Date()) + "_" + UUID.randomUUID().toString().substring(0, 8);
        String zipFileName = batchCode + ".zip";

        List<Map<String, Object>> configRecords = new ArrayList<>();
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("source_file", sourceFileName);
        record.put("table_type", "hive");
        record.put("db_name", result.getDbName() != null ? result.getDbName() : "");
        record.put("table_name", result.getTableName() != null ? result.getTableName() : "");
        record.put("is_overseas", 0);
        record.put("field_mapping", result.getFieldMappingJson() != null ? result.getFieldMappingJson() : "");
        record.put("load_mode", "partitioned-append");
        String ptDt = result.getPtDt();
        record.put("partition_info", ptDt != null && !ptDt.isEmpty() ? "pt_dt='" + ptDt + "'" : "pt_dt='2022-01-01'");
        record.put("executor_num", 4);
        record.put("executor_cores", 4);
        record.put("executor_memory", "8G");
        record.put("driver_num", 2);
        record.put("driver_memory", "2G");
        configRecords.add(record);

        String localTempDir = System.getProperty("java.io.tmpdir") + File.separator + "packaging";
        new File(localTempDir).mkdirs();

        String localConfigExcel = localTempDir + File.separator + "config_" + batchCode + ".xlsx";
        String localZipPath = localTempDir + File.separator + zipFileName;

        try {
            ConfigExcelWriter.write(localConfigExcel, configRecords);
            List<String> files = Arrays.asList(standardExcelPath, localConfigExcel);
            ZipPackager.packageFiles(localZipPath, files);

            uploadToFtp(ftpConfig, localZipPath, stagingDir + "/" + zipFileName);

            BatchRecord batch = new BatchRecord();
            batch.setBatchCode(batchCode);
            batch.setFtpConfigId(ftpConfigId);
            batch.setZipFileName(zipFileName);
            batch.setFileCount(1);
            batch.setTotalSize(new File(localZipPath).length());
            batch.setStatus("CREATED");
            batch.setCreateTime(new Date());
            batch.setUpdateTime(new Date());
            batchRecordMapper.insert(batch);

            new File(localConfigExcel).delete();
            new File(localZipPath).delete();

            log.info("Packaged and uploaded to staging: {}", zipFileName);
            return stagingDir + "/" + zipFileName;

        } catch (Exception e) {
            log.error("Failed to package files", e);
            throw new RuntimeException("Failed to package files: " + e.getMessage(), e);
        }
    }

    private void uploadToFtp(FtpConfig ftpConfig, String localPath, String remotePath) throws Exception {
        FTPClient ftpClient = null;
        FileInputStream fis = null;
        try {
            ftpClient = FtpUtil.connect(ftpConfig);
            String remoteDir = remotePath.substring(0, remotePath.lastIndexOf('/'));
            ftpClient.makeDirectory(remoteDir);
            fis = new FileInputStream(localPath);
            boolean stored = ftpClient.storeFile(remotePath, fis);
            if (!stored) {
                throw new RuntimeException("FTP store failed: " + remotePath);
            }
        } finally {
            if (fis != null) fis.close();
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
}