package com.agent.exception;

import com.agent.response.ApiResponse;
import com.openai.errors.OpenAIIoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;

/**
 * 系统全局异常处理器。
 *
 * 负责将控制器和业务层抛出的异常转换为统一接口响应。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理明确的业务异常。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        log.warn("业务处理失败，code={}，message={}", exception.getCode(), exception.getMessage());
        ApiResponse<Void> response = ApiResponse.failure(exception.getCode(), exception.getMessage());
        return ResponseEntity.status(exception.getHttpStatus()).body(response);
    }

    /**
     * 处理 @Valid 参数校验失败。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidationException(
            MethodArgumentNotValidException exception) {
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
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMessageNotReadableException(
            HttpMessageNotReadableException exception) {
        return ApiResponse.failure(40002, "请求体格式错误");
    }

    /**
     * 处理没有被单独处理的系统异常。
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception exception) {
        log.error("系统发生未处理异常", exception);
        return ApiResponse.failure(50000, "系统内部错误");
    }

    /**
     * 处理Agent工具执行异常。
     *
     * 如果工具内部抛出业务异常，则返回具体业务信息；
     * 其他工具异常统一作为系统异常处理。
     */
    @ExceptionHandler(ToolExecutionException.class)
    public ResponseEntity<ApiResponse<Void>> handleToolExecutionException(ToolExecutionException exception) {
        Throwable cause = exception.getCause();
        if (cause instanceof BusinessException businessException) {
            return handleBusinessException(businessException);
        }
        log.error("Agent工具执行失败，toolName={}", exception.getToolDefinition().name(), exception);
        ApiResponse<Void> response = ApiResponse.failure(50000, "系统内部错误");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * 处理模型客户端抛出的网络异常。
     * 网络异常不一定是超时，需要继续检查原因。
     */
    /**
     * 处理模型客户端的网络异常。
     * 超时与其他连接故障分别返回，不向用户暴露内部异常详情。
     */
    @ExceptionHandler(OpenAIIoException.class)
    public ResponseEntity<ApiResponse<Void>> handleModelIoException(OpenAIIoException exception) {
        if (isModelTimeout(exception)) {
            log.warn("模型请求超时", exception);

            ApiResponse<Void> response = ApiResponse.failure(50401, "模型响应超时，请稍后重试");
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(response);
        }

        // 例如连接被拒绝、连接中断等网络故障。
        // 客户端完成配置允许的重试后，异常才会继续向上抛出。
        log.error("模型网络请求失败", exception);

        ApiResponse<Void> response = ApiResponse.failure(50301, "模型服务暂时不可用，请稍后重试");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * 沿异常链查找超时原因。
     */
    private boolean isModelTimeout(Throwable exception) {
        Throwable current = exception;

        // 限制遍历层数，避免异常链异常时无限循环。
        for (int depth = 0; current != null && depth < 20; depth++) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }

            // 当前 OkHttp 版本的整体请求超时，就是日志里的这种异常。
            // InterruptedIOException 也可能表示其他中断，因此不能只判断类型。
            if (current instanceof InterruptedIOException
                    && "timeout".equalsIgnoreCase(current.getMessage())) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }
}