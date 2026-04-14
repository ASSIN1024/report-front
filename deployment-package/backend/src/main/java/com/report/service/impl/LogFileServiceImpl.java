package com.report.service.impl;

import com.report.dto.LogLineDTO;
import com.report.dto.LogQueryDTO;
import com.report.dto.LogResultDTO;
import com.report.service.LogFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class LogFileServiceImpl implements LogFileService {

    @Value("${logging.file.path:./logs}")
    private String logPath;

    @Value("${spring.application.name:report-platform}")
    private String appName;

    private static final Pattern LOG_PATTERN = Pattern.compile(
        "^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}) \\[(\\w+)\\] (\\w+)\\s+(\\S+) - (.+)$"
    );

    private static final Map<String, String> LOG_TYPE_MAP = new HashMap<>();
    static {
        LOG_TYPE_MAP.put("all", "report-platform.log");
        LOG_TYPE_MAP.put("error", "report-platform-error.log");
        LOG_TYPE_MAP.put("operation", "report-platform-operation.log");
        LOG_TYPE_MAP.put("access", "report-platform-access.log");
    }

    @Override
    public LogResultDTO queryLogs(LogQueryDTO query) {
        LogResultDTO result = new LogResultDTO();
        String fileName = getLogFileName(query.getLogType());
        Path filePath = Paths.get(logPath, fileName);
        
        if (!Files.exists(filePath)) {
            result.setTotalLines(0);
            result.setLines(new ArrayList<>());
            result.setTotal(0);
            return result;
        }
        
        try {
            result.setFileName(fileName);
            result.setFileSize(Files.size(filePath));
            result.setLogType(query.getLogType());
            
            List<LogLineDTO> allLines = readAndParseLog(filePath, query);
            
            int total = allLines.size();
            result.setTotal(total);
            result.setTotalLines(total);
            
            int pageNum = query.getPageNum() != null ? query.getPageNum() : 1;
            int pageSize = query.getPageSize() != null ? query.getPageSize() : 100;
            int start = (pageNum - 1) * pageSize;
            int end = Math.min(start + pageSize, total);
            
            List<LogLineDTO> pagedLines = start < total ? allLines.subList(start, end) : new ArrayList<>();
            result.setLines(pagedLines);
            result.setPageNum(pageNum);
            result.setPageSize(pageSize);
            
        } catch (IOException e) {
            log.error("读取日志文件失败: {}", e.getMessage(), e);
        }
        
        return result;
    }

    @Override
    public List<Map<String, Object>> listLogFiles() {
        List<Map<String, Object>> files = new ArrayList<>();
        Path logDir = Paths.get(logPath);
        
        if (!Files.exists(logDir)) {
            return files;
        }
        
        try (Stream<Path> paths = Files.list(logDir)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.getFileName().toString().endsWith(".log"))
                 .forEach(p -> {
                     try {
                         Map<String, Object> fileInfo = new HashMap<>();
                         String fileName = p.getFileName().toString();
                         fileInfo.put("fileName", fileName);
                         fileInfo.put("fileSize", Files.size(p));
                         fileInfo.put("lastModified", Files.getLastModifiedTime(p).toMillis());
                         fileInfo.put("logType", detectLogType(fileName));
                         files.add(fileInfo);
                     } catch (IOException e) {
                         log.warn("获取文件信息失败: {}", p, e);
                     }
                 });
        } catch (IOException e) {
            log.error("列出日志文件失败: {}", e.getMessage(), e);
        }
        
        files.sort((a, b) -> Long.compare((Long) b.get("lastModified"), (Long) a.get("lastModified")));
        return files;
    }

    @Override
    public Map<String, Object> getFileInfo(String logType) {
        String fileName = getLogFileName(logType);
        Path filePath = Paths.get(logPath, fileName);
        
        Map<String, Object> info = new HashMap<>();
        info.put("logType", logType);
        info.put("fileName", fileName);
        
        if (Files.exists(filePath)) {
            try {
                info.put("fileSize", Files.size(filePath));
                info.put("lastModified", Files.getLastModifiedTime(filePath).toMillis());
                info.put("exists", true);
            } catch (IOException e) {
                log.warn("获取文件信息失败: {}", filePath, e);
            }
        } else {
            info.put("exists", false);
            info.put("fileSize", 0L);
        }
        
        return info;
    }

    @Override
    public byte[] exportLog(LogQueryDTO query) {
        String fileName = getLogFileName(query.getLogType());
        Path filePath = Paths.get(logPath, fileName);
        
        if (!Files.exists(filePath)) {
            return new byte[0];
        }
        
        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            log.error("导出日志文件失败: {}", e.getMessage(), e);
            return new byte[0];
        }
    }

    @Override
    public List<LogLineDTO> getRecentLogs(String logType, Integer lines) {
        String fileName = getLogFileName(logType);
        Path filePath = Paths.get(logPath, fileName);
        
        if (!Files.exists(filePath)) {
            return new ArrayList<>();
        }
        
        int lineCount = lines != null ? lines : 100;
        
        try {
            List<String> allLines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
            int start = Math.max(0, allLines.size() - lineCount);
            List<String> recentLines = allLines.subList(start, allLines.size());
            
            List<LogLineDTO> result = new ArrayList<>();
            int lineNumber = start + 1;
            for (String line : recentLines) {
                LogLineDTO dto = parseLogLine(line, lineNumber++);
                result.add(dto);
            }
            
            return result;
        } catch (IOException e) {
            log.error("读取最近日志失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private String getLogFileName(String logType) {
        if (logType == null || !LOG_TYPE_MAP.containsKey(logType)) {
            return LOG_TYPE_MAP.get("all");
        }
        return LOG_TYPE_MAP.get(logType);
    }

    private String detectLogType(String fileName) {
        for (Map.Entry<String, String> entry : LOG_TYPE_MAP.entrySet()) {
            if (entry.getValue().equals(fileName)) {
                return entry.getKey();
            }
        }
        return "unknown";
    }

    private List<LogLineDTO> readAndParseLog(Path filePath, LogQueryDTO query) throws IOException {
        List<LogLineDTO> lines = new ArrayList<>();
        
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                LogLineDTO dto = parseLogLine(line, lineNumber);
                
                if (matchesFilter(dto, query)) {
                    lines.add(dto);
                }
            }
        }
        
        return lines;
    }

    private LogLineDTO parseLogLine(String line, int lineNumber) {
        LogLineDTO dto = new LogLineDTO();
        dto.setLineNumber(lineNumber);
        dto.setRawLine(line);
        
        Matcher matcher = LOG_PATTERN.matcher(line);
        if (matcher.matches()) {
            dto.setTimestamp(matcher.group(1));
            dto.setLevel(matcher.group(3));
            dto.setLogger(matcher.group(4));
            dto.setMessage(matcher.group(5));
        } else {
            dto.setMessage(line);
        }
        
        return dto;
    }

    private boolean matchesFilter(LogLineDTO dto, LogQueryDTO query) {
        if (query.getLevel() != null && !query.getLevel().isEmpty()) {
            if (dto.getLevel() == null || !dto.getLevel().equalsIgnoreCase(query.getLevel())) {
                return false;
            }
        }
        
        if (query.getKeyword() != null && !query.getKeyword().isEmpty()) {
            String keyword = query.getKeyword().toLowerCase();
            String message = dto.getMessage() != null ? dto.getMessage().toLowerCase() : "";
            String rawLine = dto.getRawLine() != null ? dto.getRawLine().toLowerCase() : "";
            
            if (!message.contains(keyword) && !rawLine.contains(keyword)) {
                return false;
            }
        }
        
        if (query.getStartDate() != null && !query.getStartDate().isEmpty() && dto.getTimestamp() != null) {
            if (dto.getTimestamp().compareTo(query.getStartDate()) < 0) {
                return false;
            }
        }
        
        if (query.getEndDate() != null && !query.getEndDate().isEmpty() && dto.getTimestamp() != null) {
            if (dto.getTimestamp().compareTo(query.getEndDate()) > 0) {
                return false;
            }
        }
        
        return true;
    }
}
