package com.report.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LogUtil {

    private static final Logger OPERATION_LOGGER = LoggerFactory.getLogger("OPERATION_LOG");
    private static final Logger ACCESS_LOGGER = LoggerFactory.getLogger("ACCESS_LOG");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static final ThreadLocal<Map<String, Object>> CONTEXT = ThreadLocal.withInitial(ConcurrentHashMap::new);

    private LogUtil() {}

    public static void setContext(String key, Object value) {
        CONTEXT.get().put(key, value);
    }

    public static Object getContext(String key) {
        return CONTEXT.get().get(key);
    }

    public static void clearContext() {
        CONTEXT.remove();
    }

    public static void logOperation(String module, String operationType, String operationDesc,
                                    String targetId, String targetName, String beforeData,
                                    String afterData, Integer result, String errorMsg, Long duration) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(LocalDateTime.now().format(FORMATTER)).append("] ");
        sb.append("[OPERATION] ");
        sb.append("module=").append(module).append(" | ");
        sb.append("type=").append(operationType).append(" | ");
        sb.append("desc=").append(operationDesc).append(" | ");
        sb.append("targetId=").append(targetId).append(" | ");
        sb.append("targetName=").append(targetName).append(" | ");
        sb.append("result=").append(result == 1 ? "SUCCESS" : "FAILED").append(" | ");
        sb.append("duration=").append(duration).append("ms");
        
        if (beforeData != null) {
            sb.append(" | beforeData=").append(beforeData);
        }
        if (afterData != null) {
            sb.append(" | afterData=").append(afterData);
        }
        if (errorMsg != null) {
            sb.append(" | errorMsg=").append(errorMsg);
        }
        
        Map<String, Object> ctx = CONTEXT.get();
        if (ctx.containsKey("userId")) {
            sb.append(" | userId=").append(ctx.get("userId"));
        }
        if (ctx.containsKey("userName")) {
            sb.append(" | userName=").append(ctx.get("userName"));
        }
        if (ctx.containsKey("clientIp")) {
            sb.append(" | clientIp=").append(ctx.get("clientIp"));
        }
        
        if (result == 1) {
            OPERATION_LOGGER.info(sb.toString());
        } else {
            OPERATION_LOGGER.error(sb.toString());
        }
    }

    public static void logAccess(String method, String uri, String params, Integer statusCode, Long duration) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(LocalDateTime.now().format(FORMATTER)).append("] ");
        sb.append("[ACCESS] ");
        sb.append("method=").append(method).append(" | ");
        sb.append("uri=").append(uri).append(" | ");
        sb.append("status=").append(statusCode).append(" | ");
        sb.append("duration=").append(duration).append("ms");
        
        if (params != null && !params.isEmpty()) {
            sb.append(" | params=").append(params);
        }
        
        Map<String, Object> ctx = CONTEXT.get();
        if (ctx.containsKey("clientIp")) {
            sb.append(" | clientIp=").append(ctx.get("clientIp"));
        }
        
        ACCESS_LOGGER.info(sb.toString());
    }

    public static void logError(String module, String operation, String message, Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(LocalDateTime.now().format(FORMATTER)).append("] ");
        sb.append("[ERROR] ");
        sb.append("module=").append(module).append(" | ");
        sb.append("operation=").append(operation).append(" | ");
        sb.append("message=").append(message);
        
        if (throwable != null) {
            sb.append(" | exception=").append(throwable.getClass().getName());
            sb.append(" | stackTrace=").append(getStackTrace(throwable));
        }
        
        OPERATION_LOGGER.error(sb.toString());
    }

    private static String getStackTrace(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.toString());
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("\n\tat ").append(element.toString());
        }
        Throwable cause = throwable.getCause();
        while (cause != null) {
            sb.append("\nCaused by: ").append(cause.toString());
            for (StackTraceElement element : cause.getStackTrace()) {
                sb.append("\n\tat ").append(element.toString());
            }
            cause = cause.getCause();
        }
        return sb.toString();
    }
}
