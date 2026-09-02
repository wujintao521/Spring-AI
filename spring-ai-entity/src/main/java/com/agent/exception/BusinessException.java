package com.agent.exception;

import org.springframework.http.HttpStatus;

import java.util.Objects;

/**
 * 系统业务异常。
 *
 * 用于表示请求本身可以正常解析，
 * 但由于当前业务状态而无法继续处理的情况。
 */
public class BusinessException extends RuntimeException {

    private static final int DEFAULT_CODE = 5000;

    private final int code;
    private final HttpStatus httpStatus;

    /**
     * 创建默认业务异常。
     *
     * 默认业务码为5000，HTTP状态为400。
     */
    public BusinessException(String message) {
        this(DEFAULT_CODE, message, HttpStatus.BAD_REQUEST);
    }

    /**
     * 创建带有指定HTTP状态的业务异常。
     *
     * 业务码仍然使用默认值5000。
     */
    public BusinessException(String message, HttpStatus httpStatus) {
        this(DEFAULT_CODE, message, httpStatus);
    }

    /**
     * 创建包含完整错误信息的业务异常。
     *
     * 当某个错误需要前端进行特殊处理时，
     * 可以为其指定独立的业务错误码。
     */
    public BusinessException(int code, String message, HttpStatus httpStatus) {
        super(Objects.requireNonNull(message, "业务异常信息不能为空"));
        this.code = code;
        this.httpStatus = Objects.requireNonNull(
                httpStatus,
                "HTTP状态不能为空"
        );
    }

    public int getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}