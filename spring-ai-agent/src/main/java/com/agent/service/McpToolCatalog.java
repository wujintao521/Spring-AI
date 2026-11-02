package com.agent.service;

import com.agent.tool.ReadOnlyRetryMcpToolCallback;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 在内存中保存最近一次成功获取的MCP工具清单。
 * 这个类不实现ToolCallbackProvider，避免被自动配置提前读取。
 */
@Service
public class McpToolCatalog {

    private static final Logger log = LoggerFactory.getLogger(McpToolCatalog.class);

    private final SyncMcpToolCallbackProvider provider;

    // 独立线程负责获取清单，不占用聊天请求线程。
    private final ScheduledExecutorService discoveryExecutor =
            Executors.newSingleThreadScheduledExecutor();

    // 只缓存工具回调和获取时间，不缓存用户身份、请求参数或订单数据。
    // volatile保证其他线程能够看到更新后的整份清单。
    private volatile Snapshot snapshot = new Snapshot(List.of(), null);

    public McpToolCatalog(SyncMcpToolCallbackProvider provider) {
        // 构造方法只保存引用，不调用getToolCallbacks()。
        this.provider = provider;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startDiscovery() {
        // Agent启动完成后，交给独立线程立即尝试一次。
        // 每次尝试结束30秒后，再尝试更新清单。
        discoveryExecutor.scheduleWithFixedDelay(
                this::refreshTools, 0, 30, TimeUnit.SECONDS);
    }

    private void refreshTools() {
        try {
            // 告诉官方Provider：这次需要重新访问tools/list。
            provider.invalidateCache();

            // 先自动发现，再为允许重试的只读工具添加重试包装。
            ToolCallback[] callbacks = ReadOnlyRetryMcpToolCallback
                    .wrap(provider)
                    .getToolCallbacks();

            List<ToolCallback> tools = List.copyOf(Arrays.asList(callbacks));

            // 全部成功后，一次性替换整份清单。
            snapshot = new Snapshot(tools, Instant.now());

            log.info("MCP工具清单获取成功，toolCount={}", tools.size());
        } catch (Exception exception) {
            // 获取失败时保留原清单，下一次定时任务仍会继续尝试。
            log.warn("MCP工具清单获取失败，保留原清单，toolCount={}",
                    snapshot.tools().size(), exception);
        }
    }

    public List<ToolCallback> currentTools() {
        // 聊天请求只读内存，不在这里发起网络访问。
        return snapshot.tools();
    }

    public Status status() {
        Snapshot current = snapshot;
        return new Status(
                current.lastSuccessAt() != null,
                current.tools().size(),
                current.lastSuccessAt());
    }

    @PreDestroy
    public void stopDiscovery() {
        discoveryExecutor.shutdownNow();
    }

    private record Snapshot(List<ToolCallback> tools, Instant lastSuccessAt) {
    }

    // hasToolList表示成功获取过清单，不表示远端当前一定在线。
    public record Status(boolean hasToolList, int toolCount, Instant lastSuccessAt) {
    }
}
