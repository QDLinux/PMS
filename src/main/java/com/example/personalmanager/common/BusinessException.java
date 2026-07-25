package com.example.personalmanager.common;

/**
 * 业务异常，用于在业务校验失败时抛出并由全局异常处理器统一转换为响应。
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
