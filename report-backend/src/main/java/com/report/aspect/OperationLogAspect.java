package com.report.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.report.annotation.OperationLogAnnotation;
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

@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    @Autowired
    private OperationLogService operationLogService;

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
        targetId = extractId(args);

        Object returnValue = null;
        try {
            returnValue = point.proceed();
            result = 1;

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
            if (arg instanceof Integer) {
                return String.valueOf(arg);
            }
            if (arg instanceof String) {
                return (String) arg;
            }
        }
        return null;
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
