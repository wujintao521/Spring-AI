package com.agent.controller;

import com.agent.response.ApiResponse;
import com.agent.service.McpTimeService;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mcp")
public class McpDemoController {

    private final McpTimeService mcpTimeService;

    public McpDemoController(McpTimeService mcpTimeService) {
        this.mcpTimeService = mcpTimeService;
    }

    /**
     * 查看远端提供的工具。
     */
    @GetMapping("/tools")
    public ApiResponse<McpSchema.ListToolsResult> listTools() {
        return ApiResponse.success(mcpTimeService.listTools());
    }

    /**
     * 调用远端时间工具。
     */
    @GetMapping("/time")
    public ApiResponse<McpSchema.CallToolResult> getCurrentTime() {
        return ApiResponse.success(mcpTimeService.getCurrentTime());
    }
}