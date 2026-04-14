package com.report.service;

import com.report.dto.LogLineDTO;
import com.report.dto.LogQueryDTO;
import com.report.dto.LogResultDTO;

import java.util.List;
import java.util.Map;

public interface LogFileService {

    LogResultDTO queryLogs(LogQueryDTO query);

    List<Map<String, Object>> listLogFiles();

    Map<String, Object> getFileInfo(String logType);

    byte[] exportLog(LogQueryDTO query);

    List<LogLineDTO> getRecentLogs(String logType, Integer lines);
}
