package com.report.packing.generator.impl;

import com.report.entity.ProcessedFile;
import com.report.entity.ReportConfig;
import com.report.mapper.ProcessedFileMapper;
import com.report.mapper.ReportConfigMapper;
import com.report.packing.generator.ConfigTableGenerator;
import com.report.util.ConfigExcelWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;

@Slf4j
@Component
public class ConfigTableGeneratorImpl implements ConfigTableGenerator {

    @Autowired
    private ProcessedFileMapper processedFileMapper;
    @Autowired
    private ReportConfigMapper reportConfigMapper;

    @Override
    public File generate(List<Long> processedFileIds, String batchNo) {
        List<Map<String, Object>> configRecords = new ArrayList<>();

        for (Long fileId : processedFileIds) {
            ProcessedFile file = processedFileMapper.selectById(fileId);
            if (file == null) {
                log.warn("ProcessedFile not found: {}", fileId);
                continue;
            }

            ReportConfig reportConfig = null;
            if (file.getReportConfigId() != null) {
                reportConfig = reportConfigMapper.selectById(file.getReportConfigId());
            }

            Map<String, Object> record = new LinkedHashMap<>();
            record.put("source_file", file.getFileName());
            record.put("table_type", reportConfig != null ? reportConfig.getTargetTableType() : "hive");
            record.put("db_name", reportConfig != null ? reportConfig.getTargetDbName() : "");
            record.put("table_name", reportConfig != null ? reportConfig.getOutputTable() : "");
            record.put("is_overseas", reportConfig != null ? reportConfig.getIsOverseas() : 0);
            record.put("field_mapping", reportConfig != null ? reportConfig.getColumnMapping() : "");
            record.put("load_mode", reportConfig != null ? reportConfig.getLoadMode() : "partitioned-append");

            String ptDt = file.getPtDt();
            record.put("partition_info", (ptDt != null && !ptDt.trim().isEmpty()) ? "pt_dt='" + ptDt + "'" : "pt_dt='2022-01-01'");

            record.put("executor_num", reportConfig != null ? reportConfig.getSparkExecutorNum() : 4);
            record.put("executor_cores", reportConfig != null ? reportConfig.getSparkExecutorCores() : 4);
            record.put("executor_memory", reportConfig != null ? reportConfig.getSparkExecutorMemory() : "8G");
            record.put("driver_num", reportConfig != null ? reportConfig.getSparkDriverNum() : 2);
            record.put("driver_memory", reportConfig != null ? reportConfig.getSparkDriverMemory() : "2G");

            configRecords.add(record);
        }

        String localTempDir = System.getProperty("java.io.tmpdir") + File.separator + "packaging";
        new File(localTempDir).mkdirs();
        String localConfigExcel = localTempDir + File.separator + "config_" + batchNo + ".xlsx";

        try {
            ConfigExcelWriter.write(localConfigExcel, configRecords);
            log.info("Config Excel generated: {}", localConfigExcel);
            return new File(localConfigExcel);
        } catch (Exception e) {
            log.error("Failed to generate config Excel", e);
            throw new RuntimeException("Failed to generate config Excel: " + e.getMessage(), e);
        }
    }
}