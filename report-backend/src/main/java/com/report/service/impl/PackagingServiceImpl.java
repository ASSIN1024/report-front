package com.report.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.report.entity.BatchRecord;
import com.report.entity.ReportConfig;
import com.report.ftp.BuiltInFtpConfig;
import com.report.ftp.FtpConfigProvider;
import com.report.mapper.BatchRecordMapper;
import com.report.service.PackagingService;
import com.report.service.TransformResult;
import com.report.util.ConfigExcelWriter;
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
    private FtpConfigProvider ftpConfigProvider;

    @Autowired
    private BatchRecordMapper batchRecordMapper;

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMddHHmmss");
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void moveToStagingDir(String standardExcelPath, String sourceFileName, TransformResult result, ReportConfig config) {
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

            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("source_file", sourceFileName);
            metadata.put("standard_file", destFileName);
            metadata.put("db_name", result.getDbName() != null ? result.getDbName() : "");
            metadata.put("table_name", result.getTableName() != null ? result.getTableName() : "");
            metadata.put("field_mapping", result.getFieldMappingJson() != null ? result.getFieldMappingJson() : "");
            metadata.put("source_to_field_mapping", result.getSourceToFieldMapping() != null ? result.getSourceToFieldMapping() : "");
            metadata.put("pt_dt", result.getPtDt() != null ? result.getPtDt() : "");
            metadata.put("load_mode", result.getLoadMode() != null ? result.getLoadMode() : "partitioned-append");
            metadata.put("staged_at", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

            if (config != null) {
                metadata.put("table_type", config.getTargetTableType() != null ? config.getTargetTableType() : "");
                metadata.put("is_overseas", config.getIsOverseas() != null ? config.getIsOverseas() : 0);
                metadata.put("partition_info", config.getPartitionInfo() != null ? config.getPartitionInfo() : "");
                metadata.put("executor_num", config.getSparkExecutorNum() != null ? config.getSparkExecutorNum() : "");
                metadata.put("executor_cores", config.getSparkExecutorCores() != null ? config.getSparkExecutorCores() : "");
                metadata.put("executor_memory", config.getSparkExecutorMemory() != null ? config.getSparkExecutorMemory() : "");
                metadata.put("driver_num", config.getSparkDriverNum() != null ? config.getSparkDriverNum() : "");
                metadata.put("driver_memory", config.getSparkDriverMemory() != null ? config.getSparkDriverMemory() : "");
            }

            String jsonFileName = stagingDir + File.separator + destFileName + ".json";
            try (FileOutputStream fos = new FileOutputStream(jsonFileName)) {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(fos, metadata);
            }

            log.info("Moved to staging: {} -> {} (with JSON metadata)", sourceFileName, destFile.getAbsolutePath());

        } catch (Exception e) {
            log.error("Failed to move file to staging: {}", sourceFileName, e);
            throw new RuntimeException("Failed to move file to staging: " + e.getMessage(), e);
        }
    }

    @Override
    public void collectAndPackageAll() {
        String stagingTempDir = System.getProperty("java.io.tmpdir") + File.separator + "staging-temp";
        File dir = new File(stagingTempDir);
        if (!dir.exists() || !dir.isDirectory()) {
            log.info("staging-temp directory not found, nothing to package");
            return;
        }

        File[] xlsxFiles = dir.listFiles((d, name) -> name.endsWith("_standard.xlsx"));
        if (xlsxFiles == null || xlsxFiles.length == 0) {
            log.info("No files to package in staging-temp");
            return;
        }

        log.info("Found {} files to package", xlsxFiles.length);

        BuiltInFtpConfig ftpConfig = ftpConfigProvider != null ? ftpConfigProvider.getConfig() : null;
        String targetStagingDir;
        if (ftpConfig != null) {
            targetStagingDir = ftpConfig.getRootDirectory() + File.separator + "staging";
        } else {
            targetStagingDir = System.getProperty("java.io.tmpdir") + File.separator + "staging";
        }

        String batchCode = "batch_" + SDF.format(new Date()) + "_" + UUID.randomUUID().toString().substring(0, 8);
        String zipFileName = batchCode + ".zip";

        String localTempDir = System.getProperty("java.io.tmpdir") + File.separator + "packaging";
        new File(localTempDir).mkdirs();
        String localZipPath = localTempDir + File.separator + zipFileName;

        try {
            List<String> filesToPackage = new ArrayList<>();
            List<File> packagedFiles = new ArrayList<>();
            List<Map<String, Object>> configRecords = new ArrayList<>();

            for (File xlsxFile : xlsxFiles) {
                filesToPackage.add(xlsxFile.getAbsolutePath());
                packagedFiles.add(xlsxFile);

                File jsonMetaFile = new File(xlsxFile.getAbsolutePath() + ".json");
                if (jsonMetaFile.exists()) {
                    packagedFiles.add(jsonMetaFile);
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> metadata = objectMapper.readValue(jsonMetaFile, Map.class);
                        configRecords.add(metadata);
                    } catch (Exception e) {
                        log.warn("Failed to read metadata for: {}", xlsxFile.getName(), e);
                    }
                }
            }

            if (!configRecords.isEmpty()) {
                String configExcelPath = localTempDir + File.separator + "informationTemplate.xlsx";
                ConfigExcelWriter.write(configExcelPath, configRecords);
                filesToPackage.add(configExcelPath);
                packagedFiles.add(new File(configExcelPath));
                log.info("Generated informationTemplate.xlsx with {} records", configRecords.size());
            }

            ZipPackager.packageFiles(localZipPath, filesToPackage);

            File zipFile = new File(localZipPath);
            long zipSize = zipFile.length();

            Path targetDir = new File(targetStagingDir).toPath();
            Files.createDirectories(targetDir);
            Path targetPath = targetDir.resolve(zipFileName);
            Files.copy(zipFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            BatchRecord batch = new BatchRecord();
            batch.setBatchCode(batchCode);
            batch.setFtpConfigId(ftpConfig != null ? -1L : -1L);
            batch.setZipFileName(zipFileName);
            batch.setFileCount(xlsxFiles.length);
            batch.setTotalSize(zipSize);
            batch.setStatus("CREATED");
            batch.setCreateTime(new Date());
            batch.setUpdateTime(new Date());
            batchRecordMapper.insert(batch);

            zipFile.delete();

            for (File packagedFile : packagedFiles) {
                if (packagedFile.exists()) {
                    packagedFile.delete();
                }
            }

            log.info("Batch packaged: {} files -> {} (size={})", xlsxFiles.length, zipFileName, zipSize);

        } catch (Exception e) {
            log.error("Failed to package files", e);
            throw new RuntimeException("Failed to package files: " + e.getMessage(), e);
        }
    }

    @Override
    public int collectAndPackageBySizeLimit(long maxSizeBytes) {
        String stagingTempDir = System.getProperty("java.io.tmpdir") + File.separator + "staging-temp";
        File dir = new File(stagingTempDir);
        if (!dir.exists() || !dir.isDirectory()) {
            return 0;
        }

        File[] xlsxFiles = dir.listFiles((d, name) -> name.endsWith("_standard.xlsx"));
        if (xlsxFiles == null || xlsxFiles.length == 0) {
            return 0;
        }

        Arrays.sort(xlsxFiles, Comparator.comparingLong(File::lastModified));

        List<File> batch = new ArrayList<>();
        long currentSize = 0;
        for (File f : xlsxFiles) {
            if (currentSize + f.length() > maxSizeBytes && !batch.isEmpty()) {
                packageBatch(batch);
                batch.clear();
                currentSize = 0;
            }
            batch.add(f);
            currentSize += f.length();
        }
        if (!batch.isEmpty()) {
            packageBatch(batch);
        }

        return xlsxFiles.length;
    }

    private void packageBatch(List<File> files) {
        if (files.isEmpty()) return;

        BuiltInFtpConfig ftpConfig = ftpConfigProvider != null ? ftpConfigProvider.getConfig() : null;
        String targetStagingDir;
        if (ftpConfig != null) {
            targetStagingDir = ftpConfig.getRootDirectory() + File.separator + "staging";
        } else {
            targetStagingDir = System.getProperty("java.io.tmpdir") + File.separator + "staging";
        }

        String batchCode = "batch_" + SDF.format(new Date()) + "_" + UUID.randomUUID().toString().substring(0, 8);
        String zipFileName = batchCode + ".zip";

        String localTempDir = System.getProperty("java.io.tmpdir") + File.separator + "packaging";
        new File(localTempDir).mkdirs();
        String localZipPath = localTempDir + File.separator + zipFileName;

        try {
            List<String> filesToPackage = new ArrayList<>();
            List<File> packagedFiles = new ArrayList<>();
            List<Map<String, Object>> configRecords = new ArrayList<>();

            for (File xlsxFile : files) {
                filesToPackage.add(xlsxFile.getAbsolutePath());
                packagedFiles.add(xlsxFile);

                File jsonMetaFile = new File(xlsxFile.getAbsolutePath() + ".json");
                if (jsonMetaFile.exists()) {
                    packagedFiles.add(jsonMetaFile);
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> metadata = objectMapper.readValue(jsonMetaFile, Map.class);
                        configRecords.add(metadata);
                    } catch (Exception e) {
                        log.warn("Failed to read metadata for: {}", xlsxFile.getName(), e);
                    }
                }
            }

            if (!configRecords.isEmpty()) {
                String configExcelPath = localTempDir + File.separator + "informationTemplate.xlsx";
                ConfigExcelWriter.write(configExcelPath, configRecords);
                filesToPackage.add(configExcelPath);
                packagedFiles.add(new File(configExcelPath));
            }

            ZipPackager.packageFiles(localZipPath, filesToPackage);

            File zipFile = new File(localZipPath);
            long zipSize = zipFile.length();

            Path targetDir = new File(targetStagingDir).toPath();
            Files.createDirectories(targetDir);
            Path targetPath = targetDir.resolve(zipFileName);
            Files.copy(zipFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            BatchRecord batch = new BatchRecord();
            batch.setBatchCode(batchCode);
            batch.setFtpConfigId(-1L);
            batch.setZipFileName(zipFileName);
            batch.setFileCount(files.size());
            batch.setTotalSize(zipSize);
            batch.setStatus("CREATED");
            batch.setCreateTime(new Date());
            batch.setUpdateTime(new Date());
            batchRecordMapper.insert(batch);

            zipFile.delete();

            for (File packagedFile : packagedFiles) {
                if (packagedFile.exists()) {
                    packagedFile.delete();
                }
            }

            log.info("Batch packaged: {} files -> {} (size={})", files.size(), zipFileName, zipSize);

        } catch (Exception e) {
            log.error("Failed to package batch", e);
        }
    }

    @Override
    public String packageToStaging(Long ftpConfigId, String standardExcelPath, String sourceFileName, TransformResult result) {
        BuiltInFtpConfig ftpConfig = ftpConfigProvider != null ? ftpConfigProvider.getConfig() : null;
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
