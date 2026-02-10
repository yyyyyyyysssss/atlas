package com.atlas.common.core.web.exception;

import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultCode;
import com.atlas.common.core.response.ResultGenerator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.stream.Collectors;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/10 13:37
 */
@Order(Ordered.LOWEST_PRECEDENCE)
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(BusinessException.class)
    public Result<?> businessException(BusinessException e) {
        log.error("业务异常: ", e);
        return ResultGenerator.failed(e.getCode(), e.getMessage());
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(SQLException.class)
    public Result<?> sqlException(SQLException e) {
        log.error("数据库操作异常: ", e);
        return ResultGenerator.failed(ResultCode.DATABASE_ERROR);
    }

    @ExceptionHandler(IOException.class)
    public void handleIOException(IOException e) {
        if (isClientAbort(e)) {
            // 客户端主动断开，正常行为
            log.info("Client aborted file stream connection");
            return;
        }
        // 处理异常
        log.error("IO Exception: ", e);
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncRequestNotUsableException(AsyncRequestNotUsableException e) {

    }

    //参数校验异常
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handlerMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        return getValidResult(e.getBindingResult());
    }

    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(BindException.class)
    public Result<?> handlerBindException(BindException e) {
        return getValidResult(e.getBindingResult());
    }

    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<?> handleConstraintViolationException(ConstraintViolationException e) {
        // 提取校验失败的提示语
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        return ResultGenerator.failed(ResultCode.PARAM_ERROR, message);
    }

    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(ValidationException.class)
    public Result<?> handlerValidationException(ValidationException e) {
        return ResultGenerator.failed(ResultCode.PARAM_ERROR, e.getMessage());
    }

    private Result<?> getValidResult(BindingResult bindingResult) {
        String message = bindingResult.getFieldErrors().stream()
                .map(f -> f.getField() + ":" + f.getDefaultMessage())
                .collect(Collectors.joining("; ")); // 自动处理分号
        return ResultGenerator.failed(ResultCode.PARAM_ERROR, message);
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("未知异常: ", e);
        return ResultGenerator.failed(ResultCode.UNKNOWN_ERROR);
    }

    private boolean isClientAbort(Throwable e) {
        Throwable t = e;
        while (t != null) {
            if (t instanceof org.apache.catalina.connector.ClientAbortException) {
                return true;
            }
            if (t instanceof java.io.IOException) {
                String msg = t.getMessage();
                if (msg != null) {
                    // 兼容 Windows (你的堆栈) / Linux (Broken pipe) / 标准描述
                    if (msg.contains("中止") ||
                            msg.contains("aborted") ||
                            msg.contains("Broken pipe") ||
                            msg.contains("Connection reset")) {
                        return true;
                    }
                }
            }
            t = t.getCause();
        }
        return false;
    }

}
