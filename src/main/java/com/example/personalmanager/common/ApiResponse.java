package com.example.personalmanager.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一接口响应封装，包含成功标识、提示信息与业务数据。
 *
 * @param <T> 业务数据类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    // 请求是否成功
    private boolean success;
    // 提示信息
    private String message;
    // 业务数据
    private T data;

    /**
     * 构造成功响应，使用默认提示信息。
     *
     * @param data 业务数据
     * @return 成功响应
     */
    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder().success(true).message("OK").data(data).build();
    }

    /**
     * 构造带自定义提示信息的成功响应。
     *
     * @param message 提示信息
     * @param data    业务数据
     * @return 成功响应
     */
    public static <T> ApiResponse<T> ok(String message, T data) {
        return ApiResponse.<T>builder().success(true).message(message).data(data).build();
    }

    /**
     * 构造失败响应。
     *
     * @param message 失败提示信息
     * @return 失败响应
     */
    public static <T> ApiResponse<T> fail(String message) {
        return ApiResponse.<T>builder().success(false).message(message).data(null).build();
    }
}
