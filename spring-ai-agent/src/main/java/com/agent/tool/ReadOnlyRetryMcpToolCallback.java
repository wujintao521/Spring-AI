package com.agent.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.mcp.SyncMcpToolCallback;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.Set;
import java.util.concurrent.TimeoutException;

public final class ReadOnlyRetryMcpToolCallback implements ToolCallback {

    private static final Logger log =
            LoggerFactory.getLogger(ReadOnlyRetryMcpToolCallback.class);

    // 总尝试次数为2：首次调用 + 最多1次重试。
    private static final int MAX_ATTEMPTS = 2;

    private static final long RETRY_DELAY_MS = 200;

    // 只有这些明确的只读工具允许重试。
    // 以后新增的工具不会自动加入。
    private static final Set<String> RETRYABLE_TOOLS = Set.of(
            "getCurrentTime",
            "getTimeByZone",
            "queryTradeOrderByOrderNo",
            "queryTradeOrdersByCustomerName",
            "queryTradeOrderForWorkflow"
    );

    // 原来的MCP工具回调，真正的远程调用仍由它执行。
    private final ToolCallback delegate;

    private ReadOnlyRetryMcpToolCallback(ToolCallback delegate) {
        this.delegate = delegate;
    }

    /**
     * 启动时取得原有MCP回调，只包装允许重试的工具。
     *
     * 此处创建包装对象，不执行订单或时间工具。
     */
    public static ToolCallbackProvider wrap(
            SyncMcpToolCallbackProvider provider) {

        ToolCallback[] callbacks = provider.getToolCallbacks();

        for (int index = 0; index < callbacks.length; index++) {
            ToolCallback callback = callbacks[index];

            // 按远端原始工具名匹配，避免受到工具名称前缀的影响。
            if (callback instanceof SyncMcpToolCallback mcpCallback
                    && RETRYABLE_TOOLS.contains(
                    mcpCallback.getOriginalToolName())) {

                callbacks[index] =
                        new ReadOnlyRetryMcpToolCallback(callback);
            }
        }

        return ToolCallbackProvider.from(callbacks);
    }

    /**
     * 工作流根据服务端原始工具名查找工具。
     */
    public String getOriginalToolName() {
        if (delegate instanceof SyncMcpToolCallback mcpCallback) {
            return mcpCallback.getOriginalToolName();
        }

        return delegate.getToolDefinition().name();
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    /**
     * 请求执行阶段，真正进行工具调用和重试。
     */
    @Override
    public String call(String toolInput, ToolContext toolContext) {
        int attempt = 1;

        while (true) {
            log.info("MCP工具调用，toolName={}，attempt={}/{}",
                    getToolDefinition().name(),
                    attempt,
                    MAX_ATTEMPTS);

            try {
                // 两次尝试使用相同的参数和ToolContext。
                return delegate.call(toolInput, toolContext);
            } catch (ToolExecutionException exception) {

                if (attempt >= MAX_ATTEMPTS
                        || Thread.currentThread().isInterrupted()
                        || !isRetryable(exception)) {

                    // 不允许重试，或者次数已用完。
                    // 保留原异常，交给现有异常处理流程。
                    throw exception;
                }

                log.warn("MCP只读工具调用失败，准备重试，toolName={}",
                        getToolDefinition().name());

                try {
                    // 在原线程短暂等待，然后再次调用。
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw exception;
                }

                attempt++;
            }
        }
    }

    /**
     * 只重试明确的等待超时、HTTP超时、Socket超时和连接失败。
     */
    private boolean isRetryable(Throwable exception) {
        Throwable current = exception;

        for (int depth = 0; current != null && depth < 20; depth++) {
            if (current instanceof TimeoutException
                    || current instanceof HttpTimeoutException
                    || current instanceof SocketTimeoutException
                    || current instanceof ConnectException) {
                return true;
            }

            current = current.getCause();
        }

        // 参数错误、身份错误，以及普通isError=true工具结果，
        // 不具有上述异常类型，因此默认不重试。
        return false;
    }
}