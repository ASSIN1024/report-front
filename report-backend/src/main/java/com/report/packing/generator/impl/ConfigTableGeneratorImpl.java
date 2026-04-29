package com.report.packing.generator.impl;

import cn.hutool.json.JSONUtil;
import com.report.entity.ProcessedFile;
import com.report.entity.ReportConfig;
import com.report.entity.dto.ColumnMapping;
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
            record.put("field_type_json", buildFieldTypeJson(reportConfig));
            record.put("load_mode", reportConfig != null ? reportConfig.getLoadMode() : "partitioned-append");

            record.put("partition_info", buildPartitionInfo(reportConfig, file));

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

    private String buildPartitionInfo(ReportConfig reportConfig, ProcessedFile file) {
        if (reportConfig == null || reportConfig.getLoadMode() == null) {
            return "pt_dt='" + getDefaultDate() + "'";
        }

        String loadMode = reportConfig.getLoadMode();
        if (!loadMode.startsWith("partitioned")) {
            return "";
        }

        String partitionFieldName = extractPartitionFieldName(reportConfig.getPartitionInfo());
        String ptDt = file.getPtDt();

        if (ptDt != null && !ptDt.trim().isEmpty()) {
            String formattedDate = formatDateToHyphen(ptDt);
            return partitionFieldName + "='" + formattedDate + "'";
        }

        return partitionFieldName + "='" + getDefaultDate() + "'";
    }

    private String extractPartitionFieldName(String partitionInfo) {
        if (partitionInfo == null || partitionInfo.trim().isEmpty()) {
            return "pt_dt";
        }

        String trimmed = partitionInfo.trim();
        int equalIndex = trimmed.indexOf('=');
        if (equalIndex > 0) {
            return trimmed.substring(0, equalIndex).trim();
        }

        return trimmed;
    }

    private String formatDateToHyphen(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return getDefaultDate();
        }

        String cleaned = dateStr.replace("-", "").replace("/", "").trim();

        if (cleaned.length() == 8) {
            try {
                String year = cleaned.substring(0, 4);
                String month = cleaned.substring(4, 6);
                String day = cleaned.substring(6, 8);
                return year + "-" + month + "-" + day;
            } catch (Exception e) {
                log.warn("日期格式化失败: {}", dateStr);
            }
        }

        if (dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return dateStr;
        }

        return dateStr;
    }

    private String getDefaultDate() {
        java.time.LocalDate today = java.time.LocalDate.now();
        return today.toString();
    }

    private String buildFieldTypeJson(ReportConfig reportConfig) {
        if (reportConfig == null || reportConfig.getColumnMapping() == null || reportConfig.getColumnMapping().trim().isEmpty()) {
            return "{}";
        }

        try {
            List<ColumnMapping> mappings = JSONUtil.toList(reportConfig.getColumnMapping(), ColumnMapping.class);
            Map<String, String> fieldTypeMap = new LinkedHashMap<>();

            for (ColumnMapping mapping : mappings) {
                if (mapping.getFieldName() != null && !mapping.getFieldName().trim().isEmpty()) {
                    String fieldType = convertToTargetType(mapping.getFieldType());
                    fieldTypeMap.put(mapping.getFieldName(), fieldType);
                }
            }

            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(fieldTypeMap);
        } catch (Exception e) {
            log.warn("从列映射生成字段类型JSON失败: {}", e.getMessage());
            return "{}";
        }
    }

    private String convertToTargetType(String fieldType) {
        if (fieldType == null) {
            return "string";
        }

        switch (fieldType.toUpperCase()) {
            case "STRING":
                return "string";
            case "INTEGER":
                return "int";
            case "DECIMAL":
                return "double";
            case "DATE":
                return "date";
            case "DATETIME":
                return "timestamp";
            default:
                return "string";
        }
    }
}