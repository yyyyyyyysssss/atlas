package com.atlas.common.core.web.exception;

import com.atlas.common.core.exception.BaseException;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultCode;
import com.atlas.common.core.response.ResultGenerator;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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
public class GlobalExceptionAdvice {


    // Resilience4j 熔断器开启时的异常
    @ExceptionHandler({CallNotPermittedException.class, NoFallbackAvailableException.class})
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE) // 返回 503 状态码
    public Result<?> handleCircuitBreakerException(Exception e) {
        String circuitBreakerName = "Unknown";
        if (e instanceof CallNotPermittedException) {
            circuitBreakerName = ((CallNotPermittedException) e).getCausingCircuitBreakerName();
        }
        log.warn("触发系统熔断自保机制，熔断器: {}", circuitBreakerName);
        // 返回业务定义的错误结构
        return ResultGenerator.failed(ResultCode.SERVICE_UNAVAILABLE, "系统暂时无法处理此请求，请稍后再试");
    }

    // HTTP 请求体无法读取：400
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.debug("HTTP message not readable: {}", e.getMessage());
        return ResponseEntity.badRequest().build();
    }

    // HTTP 请求参数类型转换失败：400
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.debug("HTTP parameter type mismatch: {}", e.getMessage());
        return ResponseEntity.badRequest().build();
    }

    // 请求资源不存在：404
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResourceFoundException(NoResourceFoundException e) {
        log.debug("Resource not found: {}", e.getResourcePath());
        return ResponseEntity.notFound().build();
    }

    // 请求接口不存在：404
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Void> handleNoHandlerFoundException(NoHandlerFoundException e) {
        log.debug("No handler found: {}", e.getRequestURL());
        return ResponseEntity.notFound().build();
    }

    // 请求方法不支持：405
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Void> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.debug("Request method not supported: {}", e.getMethod());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }

    // 请求媒体类型不支持：415
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Void> handleMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        log.debug("Media type not supported: {}", e.getContentType());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
    }

    // 响应媒体类型不可接受：406
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Void> handleMediaTypeNotAcceptableException(HttpMediaTypeNotAcceptableException e) {
        log.debug("Media type not acceptable: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
    }

    // 上传文件超过限制：413
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("Upload size exceeded: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
    }

    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(BaseException.class)
    public Result<?> handleBaseException(BaseException e) {
        log.error("业务异常: [{}] {}, 详情: {}", e.getErrorCode().getCode(), e.getErrorCode().getMessage(), e.getDetail(), e);
        return ResultGenerator.failed(e.getErrorCode(), e.getMessage());
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


    //请求参数缺失
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<?> handlerMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        return ResultGenerator.failed(ResultCode.PARAM_ERROR,e.getMessage());
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

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("未知异常: ", e);
        return ResultGenerator.failed(ResultCode.UNKNOWN_ERROR);
    }

    private boolean isClientAbort(Throwable e) {
        Throwable t = e;
        while (t != null) {
            String className = t.getClass().getName();
            if ("org.apache.catalina.connector.ClientAbortException".equals(className)) {
                return true;
            }
            if (t instanceof java.io.IOException) {
                String msg = t.getMessage();
                if (msg != null) {
                    // 转小写处理，提高兼容性
                    String lowerMsg = msg.toLowerCase();
                    if (lowerMsg.contains("中止") ||
                            lowerMsg.contains("aborted") ||
                            lowerMsg.contains("broken pipe") ||
                            lowerMsg.contains("connection reset") ||
                            lowerMsg.contains("established connection was aborted")) { // Windows 常见错误描述
                        return true;
                    }
                }
            }
            t = t.getCause();
        }
        return false;
    }

    private Result<?> getValidResult(BindingResult bindingResult) {
        String message = bindingResult.getFieldErrors().stream()
                .map(f -> f.getField() + ":" + f.getDefaultMessage())
                .collect(Collectors.joining("; ")); // 自动处理分号
        return ResultGenerator.failed(ResultCode.PARAM_ERROR, message);
    }

}
