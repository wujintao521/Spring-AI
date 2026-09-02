package com.agent.enums;

import org.springframework.http.HttpStatus;

/**
 * 系统统一业务错误码。
 *
 * HTTP 状态码用于表示请求在协议层面的处理结果，
 * 业务错误码用于让前端准确识别具体失败原因。
 */
public enum BusinessErrorCodeEnum {

    ACTION_ID_REQUIRED(
            40003,
            "待执行动作编号不能为空",
            HttpStatus.BAD_REQUEST
    ),

    ACTION_NOT_FOUND(
            40402,
            "待执行动作不存在",
            HttpStatus.NOT_FOUND
    ),

    ACTION_NOT_CONFIRMED(
            40903,
            "待执行动作尚未确认或当前状态不允许执行",
            HttpStatus.CONFLICT
    ),

    ACTION_EXPIRED(
            40904,
            "待执行动作已经过期",
            HttpStatus.CONFLICT
    ),

    ORDER_NOT_CANCELLABLE(
            40905,
            "订单不存在或当前状态不允许取消",
            HttpStatus.CONFLICT
    ),

    ACTION_TYPE_NOT_SUPPORTED(
            40906,
            "当前执行器不支持该业务动作",
            HttpStatus.CONFLICT
    ),

    ACTION_DATA_INVALID(
            50001,
            "待执行动作数据不完整",
            HttpStatus.INTERNAL_SERVER_ERROR
    ),

    ACTION_EXECUTION_FAILED(
            50002,
            "更新待执行动作状态失败",
            HttpStatus.INTERNAL_SERVER_ERROR
    ),

    ACTION_RESULT_INCONSISTENT(
            50003,
            "动作执行结果与业务数据不一致",
            HttpStatus.INTERNAL_SERVER_ERROR
    );

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    BusinessErrorCodeEnum(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}