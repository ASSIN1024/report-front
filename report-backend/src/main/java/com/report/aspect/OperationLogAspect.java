package com.report.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.report.annotation.OperationLogAnnotation;
import com.report.entity.FtpConfig;
import com.report.service.FtpConfigService;
import com.report.service.OperationLogService;
import com.report.util.LogUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private FtpConfigService ftpConfigService;

    @Autowired
    private ObjectMapper objectMapper;

    @Around("@annotation(com.report.annotation.OperationLogAnnotation)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        OperationLogAnnotation annotation = method.getAnnotation(OperationLogAnnotation.class);
        
        String module = annotation.module();
        String operationType = annotation.operationType();
        String operationDesc = annotation.operationDesc();
        
        String targetId = null;
        String targetName = null;
        String beforeData = null;
        String afterData = null;
        Integer result = 1;
        String errorMsg = null;
        
        String operatorIp = getOperatorIp();
        LogUtil.setContext("clientIp", operatorIp);
        
        Object[] args = point.getArgs();
        
        if ("UPDATE".equals(operationType) || "DELETE".equals(operationType) || "TEST".equals(operationType)) {
            targetId = extractId(args);
            if (targetId != null) {
                try {
                    FtpConfig beforeConfig = ftpConfigService.getById(Long.parseLong(targetId));
                    if (beforeConfig != null) {
                        targetName = beforeConfig.getConfigName();
                        beforeData = objectMapper.writeValueAsString(maskPassword(beforeConfig));
                    }
                } catch (Exception e) {
                    log.warn("获取操作前数据失败: {}", e.getMessage());
                }
            }
        }
        
        Object returnValue = null;
        try {
            returnValue = point.proceed();
            result = 1;
            
            if ("UPDATE".equals(operationType) && targetId != null) {
                try {
                    FtpConfig afterConfig = ftpConfigService.getById(Long.parseLong(targetId));
                    if (afterConfig != null) {
                        afterData = objectMapper.writeValueAsString(maskPassword(afterConfig));
                    }
                } catch (Exception e) {
                    log.warn("获取操作后数据失败: {}", e.getMessage());
                }
            }
            
            if ("TEST".equals(operationType) && returnValue != null) {
                if (returnValue instanceof com.report.common.result.Result) {
                    com.report.common.result.Result<?> res = (com.report.common.result.Result<?>) returnValue;
                    if (res.getData() instanceof Boolean) {
                        result = (Boolean) res.getData() ? 1 : 0;
                        if (result == 0) {
                            errorMsg = "连接测试失败";
                        }
                    }
                }
            }
            
        } catch (Throwable e) {
            result = 0;
            errorMsg = e.getMessage();
            LogUtil.logError(module, operationDesc, errorMsg, e);
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            String operatorName = getOperatorName();
            
            operationLogService.log(module, operationType, operationDesc, targetId, targetName,
                    beforeData, afterData, result, errorMsg, duration);
            
            LogUtil.logOperation(module, operationType, operationDesc, targetId, targetName,
                    beforeData, afterData, result, errorMsg, duration);
            
            LogUtil.clearContext();
        }
        
        return returnValue;
    }
    
    private String extractId(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        
        for (Object arg : args) {
            if (arg instanceof Long) {
                return String.valueOf(arg);
            }
            if (arg instanceof FtpConfig) {
                FtpConfig config = (FtpConfig) arg;
                return config.getId() != null ? String.valueOf(config.getId()) : null;
            }
        }
        return null;
    }
    
    private Map<String, Object> maskPassword(FtpConfig config) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", config.getId());
        map.put("configName", config.getConfigName());
        map.put("host", config.getHost());
        map.put("port", config.getPort());
        map.put("username", config.getUsername());
        map.put("password", "******");
        map.put("scanPath", config.getScanPath());
        map.put("filePattern", config.getFilePattern());
        map.put("scanInterval", config.getScanInterval());
        map.put("status", config.getStatus());
        map.put("remark", config.getRemark());
        return map;
    }
    
    private String getOperatorIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("X-Real-IP");
                }
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getRemoteAddr();
                }
                return ip;
            }
        } catch (Exception e) {
            log.warn("获取操作者IP失败: {}", e.getMessage());
        }
        return "unknown";
    }
    
    private String getOperatorName() {
        return "system";
    }
}
