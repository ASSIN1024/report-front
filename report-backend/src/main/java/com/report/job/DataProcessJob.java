package com.report.job;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.report.common.constant.FieldTypeEnum;
import com.report.entity.ReportConfig;
import com.report.entity.TaskExecution;
import com.report.entity.dto.ColumnMapping;
import com.report.entity.dto.FieldMapping;
import com.report.mapper.TaskExecutionMapper;
import com.report.service.LogService;
import com.report.service.ReportConfigService;
import com.report.service.TableCreatorService;
import com.report.service.TaskService;
import com.report.util.ExcelUtil;
import com.report.util.FtpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Component
public class DataProcessJob {

    @Autowired
    private TaskService taskService;

    @Autowired
    private ReportConfigService reportConfigService;

    @Autowired
    private LogService logService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TaskExecutionMapper taskExecutionMapper;

    @Autowired
    private TableCreatorService tableCreatorService;

    private static final String TASK_TYPE_FILE_PROCESS = "FILE_PROCESS";
    private static final String TASK_TYPE_DATA_IMPORT = "DATA_IMPORT";

    public void processFile(Long taskId, ReportConfig reportConfig, File file) {
        TaskExecution task = taskService.getById(taskId);
        if (task == null) {
            log.error("任务不存在: {}", taskId);
            return;
        }

        taskService.updateTaskStatus(taskId, "RUNNING");
        logService.saveLog(taskId, "INFO", "开始处理文件: " + file.getName());

        int totalRows = 0;
        int successRows = 0;
        int failedRows = 0;

        try {
            List<FieldMapping> columnMappings = parseColumnMapping(reportConfig.getColumnMapping());
            if (columnMappings == null || columnMappings.isEmpty()) {
                throw new RuntimeException("列映射配置为空");
            }

            String outputTable = reportConfig.getOutputTable();
            logService.saveLog(taskId, "INFO", "检查目标表: " + outputTable);
            tableCreatorService.ensureTableExists(outputTable, columnMappings);

            List<Map<String, Object>> dataList = ExcelUtil.readExcel(file, reportConfig, columnMappings);
            totalRows = dataList.size();
            logService.saveLog(taskId, "INFO", "解析Excel完成，共 " + totalRows + " 行数据");

            if ("APPEND".equals(reportConfig.getOutputMode())) {
                successRows = insertData(taskId, reportConfig, dataList, columnMappings);
                failedRows = totalRows - successRows;
            } else if ("OVERWRITE".equals(reportConfig.getOutputMode())) {
                logService.saveLog(taskId, "INFO", "清空目标表: " + reportConfig.getOutputTable());
                jdbcTemplate.execute("DELETE FROM " + reportConfig.getOutputTable());
                successRows = insertData(taskId, reportConfig, dataList, columnMappings);
                failedRows = totalRows - successRows;
            }

            taskService.updateTaskProgress(taskId, totalRows, successRows, failedRows);
            taskService.finishTask(taskId, "SUCCESS", null);
            logService.saveLog(taskId, "INFO", "文件处理完成，成功: " + successRows + " 行，失败: " + failedRows + " 行");

        } catch (Exception e) {
            log.error("文件处理异常: {}", file.getName(), e);
            taskService.finishTask(taskId, "FAILED", e.getMessage());
            logService.saveLog(taskId, "ERROR", "文件处理失败: " + e.getMessage());
        }
    }

    private List<FieldMapping> parseColumnMapping(String columnMappingJson) {
        if (StrUtil.isBlank(columnMappingJson)) {
            return Collections.emptyList();
        }
        try {
            return ExcelUtil.parseColumnMapping(columnMappingJson);
        } catch (Exception e) {
            log.error("解析列映射配置失败: {}", columnMappingJson, e);
            return Collections.emptyList();
        }
    }

    private int insertData(Long taskId, ReportConfig reportConfig, List<Map<String, Object>> dataList,
                           List<FieldMapping> columnMappings) {
        String tableName = reportConfig.getOutputTable();
        int successCount = 0;

        List<String> columns = new ArrayList<>();
        List<String> placeholders = new ArrayList<>();
        List<String> fields = new ArrayList<>();

        columns.add("pt_dt");
        placeholders.add("?");
        fields.add("pt_dt");

        for (FieldMapping mapping : columnMappings) {
            columns.add(mapping.getFieldName());
            placeholders.add("?");
            fields.add(mapping.getFieldName());
        }

        String sql = String.format("INSERT INTO %s (%s) VALUES (%s)",
                tableName, String.join(", ", columns), String.join(", ", placeholders));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        for (Map<String, Object> row : dataList) {
            try {
                List<Object> values = new ArrayList<>();
                values.add(sdf.format(new Date()));

                for (FieldMapping mapping : columnMappings) {
                    Object value = row.get(mapping.getFieldName());
                    value = convertValue(value, mapping.getFieldType(), mapping.getDateFormat());
                    values.add(value);
                }

                jdbcTemplate.update(sql, values.toArray());
                successCount++;
            } catch (Exception e) {
                log.error("插入数据失败: row={}", row, e);
                logService.saveLog(taskId, "WARN", "数据插入失败: " + e.getMessage());
            }
        }

        return successCount;
    }

    private Object convertValue(Object value, String fieldType, String dateFormat) {
        if (value == null) {
            return null;
        }

        if (StrUtil.isBlank(String.valueOf(value))) {
            return null;
        }

        String strValue = String.valueOf(value).trim();

        try {
            switch (fieldType) {
                case "STRING":
                    return strValue;
                case "INTEGER":
                    if (strValue.contains(".")) {
                        return new BigDecimal(strValue).intValue();
                    }
                    return Long.parseLong(strValue);
                case "DECIMAL":
                    return new BigDecimal(strValue);
                case "DATE":
                    return parseDate(strValue, dateFormat);
                case "DATETIME":
                    return parseDatetime(strValue, dateFormat);
                default:
                    return strValue;
            }
        } catch (Exception e) {
            log.warn("数据类型转换失败: value={}, type={}", value, fieldType, e);
            return strValue;
        }
    }

    private Object parseDate(String strValue, String dateFormat) {
        String[] formats;
        if (StrUtil.isNotBlank(dateFormat)) {
            formats = new String[]{dateFormat, "yyyy-MM-dd", "yyyy/MM/dd"};
        } else {
            formats = new String[]{"yyyy-MM-dd", "yyyy/MM/dd", "yyyy-MM-dd HH:mm:ss"};
        }
        
        for (String fmt : formats) {
            try {
                java.util.Date dateValue = new SimpleDateFormat(fmt).parse(strValue);
                return new java.sql.Date(dateValue.getTime());
            } catch (Exception ignored) {
            }
        }
        throw new RuntimeException("无法解析日期: " + strValue);
    }

    private Object parseDatetime(String strValue, String dateFormat) {
        String[] formats;
        if (StrUtil.isNotBlank(dateFormat)) {
            formats = new String[]{dateFormat, "yyyy-MM-dd HH:mm:ss", "yyyy/MM/dd HH:mm:ss"};
        } else {
            formats = new String[]{"yyyy-MM-dd HH:mm:ss", "yyyy/MM/dd HH:mm:ss", "yyyy-MM-dd"};
        }
        
        for (String fmt : formats) {
            try {
                java.util.Date datetimeValue = new SimpleDateFormat(fmt).parse(strValue);
                return new java.sql.Timestamp(datetimeValue.getTime());
            } catch (Exception ignored) {
            }
        }
        throw new RuntimeException("无法解析日期时间: " + strValue);
    }

    public void retryTask(Long taskId) {
        TaskExecution task = taskService.getById(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }

        ReportConfig reportConfig = reportConfigService.getById(task.getReportConfigId());
        if (reportConfig == null) {
            throw new RuntimeException("报表配置不存在");
        }

        String filePath = task.getFilePath();
        File file = new File(filePath);
        if (!file.exists()) {
            throw new RuntimeException("文件不存在: " + filePath);
        }

        taskService.updateTaskStatus(taskId, "PENDING");
        processFile(taskId, reportConfig, file);
    }
}
