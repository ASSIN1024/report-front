package com.report.common.exception;

import com.baomidou.mybatisplus.core.exceptions.MybatisPlusException;
import com.report.common.constant.ErrorCode;
import com.report.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.error("业务异常: {}", e.getMessage(), e);
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.error("参数校验异常: {}", message);
        return Result.error(ErrorCode.BAD_REQUEST, message);
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.error("参数绑定异常: {}", message);
        return Result.error(ErrorCode.BAD_REQUEST, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        log.error("约束校验异常: {}", message);
        return Result.error(ErrorCode.BAD_REQUEST, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.error("请求体解析异常: {}", e.getMessage());
        String message = "请求参数格式错误";
        if (e.getCause() != null && e.getCause().getMessage() != null) {
            String causeMsg = e.getCause().getMessage();
            if (causeMsg.contains("Cannot deserialize value of type")) {
                message = "参数类型转换失败，请检查参数格式";
            } else if (causeMsg.contains("Unrecognized field")) {
                message = "请求包含未知字段";
            }
        }
        return Result.error(ErrorCode.BAD_REQUEST, message);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicateKeyException(DuplicateKeyException e) {
        log.error("数据重复异常: {}", e.getMessage());
        String message = "数据已存在，请检查编码或名称是否重复";
        if (e.getCause() != null && e.getCause() instanceof SQLIntegrityConstraintViolationException) {
            String sqlMsg = e.getCause().getMessage();
            if (sqlMsg.contains("report_code")) {
                message = "报表编码已存在，请使用其他编码";
            } else if (sqlMsg.contains("config_name")) {
                message = "配置名称已存在，请使用其他名称";
            }
        }
        return Result.error(ErrorCode.BAD_REQUEST, message);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result<Void> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        log.error("数据完整性异常: {}", e.getMessage());
        String message = "数据操作失败";
        Throwable rootCause = getRootCause(e);
        if (rootCause != null) {
            String sqlMsg = rootCause.getMessage();
            if (sqlMsg != null) {
                if (sqlMsg.contains("foreign key constraint")) {
                    message = "关联数据不存在，请检查FTP配置是否正确";
                } else if (sqlMsg.contains("doesn't have a default value")) {
                    String field = extractFieldName(sqlMsg, "Field '");
                    if (field != null) {
                        message = "必填字段[" + field + "]不能为空";
                    } else {
                        message = "必填字段不能为空";
                    }
                } else if (sqlMsg.contains("cannot be null")) {
                    message = "必填字段不能为空";
                } else if (sqlMsg.contains("Duplicate entry")) {
                    if (sqlMsg.contains("report_code")) {
                        message = "报表编码已存在，请使用其他编码";
                    } else if (sqlMsg.contains("config_name")) {
                        message = "配置名称已存在，请使用其他名称";
                    } else {
                        message = "数据已存在";
                    }
                }
            }
        }
        return Result.error(ErrorCode.BAD_REQUEST, message);
    }

    private Throwable getRootCause(Throwable e) {
        Throwable cause = e;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }

    private String extractFieldName(String message, String prefix) {
        int start = message.indexOf(prefix);
        if (start == -1) return null;
        start += prefix.length();
        int end = message.indexOf("'", start);
        if (end == -1) return null;
        return message.substring(start, end);
    }

    @ExceptionHandler(MybatisPlusException.class)
    public Result<Void> handleMybatisPlusException(MybatisPlusException e) {
        log.error("数据库操作异常: {}", e.getMessage(), e);
        return Result.error(ErrorCode.DB_OPERATE_ERROR, "数据库操作失败: " + e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("参数异常: {}", e.getMessage());
        return Result.error(ErrorCode.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(NumberFormatException.class)
    public Result<Void> handleNumberFormatException(NumberFormatException e) {
        log.error("数字格式异常: {}", e.getMessage());
        return Result.error(ErrorCode.BAD_REQUEST, "ID格式错误，请检查参数是否有效");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.error("方法参数类型不匹配: {}", e.getMessage());
        if (e.getMessage().contains("Long")) {
            return Result.error(ErrorCode.BAD_REQUEST, "ID格式错误，请检查参数是否有效");
        }
        return Result.error(ErrorCode.BAD_REQUEST, "参数类型错误: " + e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e);
        return Result.error(ErrorCode.INTERNAL_ERROR, "系统异常，请联系管理员");
    }
}
