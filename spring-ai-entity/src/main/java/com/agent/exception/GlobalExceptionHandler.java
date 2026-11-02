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
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.TimeoutException;

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

    /**
     * 处理 Agent 工具执行异常。
     *
     * 本地工具和 MCP 工具都可能进入这里，
     * 因此提示统一使用“工具”，不写死为某个 MCP 服务。
     */
    @ExceptionHandler(ToolExecutionException.class)
    public ResponseEntity<ApiResponse<Void>> handleToolExecutionException(ToolExecutionException exception) {

        // 保留原有逻辑：本地工具直接包装的业务异常，
        // 继续按照业务异常中的状态码和提示返回。
        Throwable cause = exception.getCause();
        if (cause instanceof BusinessException businessException) {
            return handleBusinessException(businessException);
        }

        String toolName = exception.getToolDefinition().name();

        // 必须先判断超时，因为 SocketTimeoutException
        // 本身也是 SocketException 的子类。
        if (isToolTimeout(exception)) {
            log.warn("工具调用超时，toolName={}", toolName, exception);

            ApiResponse<Void> response = ApiResponse.failure(
                    50402, "工具响应超时，请稍后重试");

            return ResponseEntity
                    .status(HttpStatus.GATEWAY_TIMEOUT)
                    .body(response);
        }

        if (isToolConnectionFailure(exception)) {
            log.error("工具连接失败，toolName={}", toolName, exception);

            ApiResponse<Void> response = ApiResponse.failure(
                    50302, "工具服务连接异常，请稍后重试");

            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(response);
        }

        // 包括 MCP 返回 isError=true 后产生的普通工具异常。
        // 未明确分类的错误继续兜底处理，不直接向前端暴露内部详情。
        log.error("Agent工具执行失败，toolName={}", toolName, exception);

        ApiResponse<Void> response =
                ApiResponse.failure(50000, "系统内部错误");

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    /**
     * 判断异常链中是否存在明确的超时异常。
     */
    private boolean isToolTimeout(Throwable exception) {
        return hasCause(exception, TimeoutException.class)
                || hasCause(exception, HttpTimeoutException.class)
                || hasCause(exception, SocketTimeoutException.class);
    }

    /**
     * 判断异常链中是否存在明确的连接故障。
     */
    private boolean isToolConnectionFailure(Throwable exception) {
        // SocketException 包含 ConnectException（例如连接被拒绝），
        // 也可能表示连接被重置等故障。
        // UnknownHostException 表示域名解析失败。
        return hasCause(exception, SocketException.class)
                || hasCause(exception, UnknownHostException.class);
    }

    /**
     * 从当前异常开始，沿 cause 查找指定异常类型。
     */
    private boolean hasCause(Throwable exception, Class<? extends Throwable> expectedType) {
        Throwable current = exception;
        // 限制遍历层数，避免异常链异常时无限循环。
        for (int depth = 0; current != null && depth < 20; depth++) {
            if (expectedType.isInstance(current)) {
                return true;
            }

            current = current.getCause();
        }
        return false;
    }
}