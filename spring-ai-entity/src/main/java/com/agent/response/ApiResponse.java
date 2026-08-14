package com.agent.response;

import java.time.Instant;

/**
 * 所有 HTTP 接口统一使用的响应格式。
 *
 * @param code      业务状态码，0 表示成功
 * @param message   响应说明
 * @param data      具体业务数据
 * @param timestamp 服务器生成响应的时间
 * @param <T>       具体业务数据类型
 */
public record ApiResponse<T>(
        int code,
        String message,
        T data,
        Instant timestamp
) {

    private static final int SUCCESS_CODE = 0;
    private static final String SUCCESS_MESSAGE = "success";

    /**
     * 创建成功响应。
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
                SUCCESS_CODE,
                SUCCESS_MESSAGE,
                data,
                Instant.now()
        );
    }

    /**
     * 创建失败响应。
     *
     * 目前先提供这个方法，之后做全局异常处理时再使用。
     */
    public static <T> ApiResponse<T> failure(int code, String message) {
        return new ApiResponse<>(
                code,
                message,
                null,
                Instant.now()
        );
    }
}