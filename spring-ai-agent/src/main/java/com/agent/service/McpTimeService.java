package com.agent.service;

import com.agent.exception.BusinessException;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class McpTimeService {

    private final McpSyncClient mcpClient;

    public McpTimeService(List<McpSyncClient> mcpClients) {
        // Starter根据YAML中的连接配置创建客户端。
        // 当前练习只配置了一个MCP Server，因此要求恰好一个客户端。
        if (mcpClients.size() != 1) {
            throw new IllegalStateException("当前时间工具练习要求配置一个MCP客户端");
        }

        this.mcpClient = mcpClients.get(0);
    }

    /**
     * 查询远端MCP Server的工具列表。
     */
    public McpSchema.ListToolsResult listTools() {
        // SDK负责组织tools/list协议请求并解析响应。
        return mcpClient.listTools();
    }

    /**
     * 明确调用远端的时间工具。
     */
    public McpSchema.CallToolResult getCurrentTime() {
        McpSchema.CallToolRequest request = McpSchema.CallToolRequest.builder()
                .name("getCurrentTime")
                // 这个工具没有入参，因此传空参数对象。
                .arguments(Map.of())
                .build();

        // SDK负责组织tools/call请求并发送到8090的MCP Server。
        McpSchema.CallToolResult result = mcpClient.callTool(request);

        // 收到了协议响应，不代表工具业务一定执行成功。
        if (result == null || Boolean.TRUE.equals(result.isError())) {
            throw new BusinessException("MCP时间工具执行失败");
        }

        return result;
    }
}