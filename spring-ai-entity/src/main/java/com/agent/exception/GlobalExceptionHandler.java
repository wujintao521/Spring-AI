package com.agent.exception;

import com.agent.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理 @Valid 参数校验失败。
     *
     * 例如：
     * message 为空
     * message 长度超过限制
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidationException(
            MethodArgumentNotValidException exception
    ) {
        String message = exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(fieldError -> fieldError.getDefaultMessage())
                .orElse("请求参数不合法");

        return ApiResponse.failure(40001, message);
    }

    /**
     * 处理请求体无法解析的异常。
     *
     * 例如：
     * JSON 格式错误
     * 请求体为空
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMessageNotReadableException(
            HttpMessageNotReadableException exception
    ) {
        return ApiResponse.failure(
                40002,
                "请求体格式错误"
        );
    }

    /**
     * 处理其他没有被单独处理的异常。
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception exception) {
        // 完整异常只记录在服务端日志中，不能直接返回给前端
        log.error("系统发生未处理异常", exception);

        return ApiResponse.failure(
                50000,
                "系统内部错误"
        );
    }
}
