package com.example.personalmanager.common;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器，统一将各类异常转换为标准的 {@link ApiResponse} 响应。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理请求体参数校验失败异常（@Valid），拼接所有字段错误信息后返回 400。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> {
                    // 字段级错误拼接「字段名: 错误信息」，其余错误仅取默认信息
                    if (error instanceof FieldError fieldError) {
                        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
                    }
                    return error.getDefaultMessage();
                })
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(ApiResponse.fail(message));
    }

    /**
     * 处理约束校验失败异常（如方法参数校验），返回 400。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraint(ConstraintViolationException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
    }

    /**
     * 处理业务异常，返回 400 及业务提示信息。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(ex.getMessage()));
    }

    /**
     * 处理凭证错误异常（用户名或密码不正确），返回 401。
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.fail("用户名或密码错误"));
    }

    /**
     * 处理其余认证异常，返回 401。
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuth(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.fail("认证失败"));
    }

    /**
     * 兜底处理未捕获的异常，返回 500。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("服务器内部错误: " + ex.getMessage()));
    }
}
