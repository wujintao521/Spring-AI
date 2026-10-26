package com.agent.controller;

import com.agent.request.AgentChatRequest;
import com.agent.response.AgentChatResponse;
import com.agent.response.ApiResponse;
import com.agent.service.AgentService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agent HTTP 接口。
 */
@RestController
@RequestMapping("/api/agent")
@Slf4j
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/chat")
    public ApiResponse<AgentChatResponse> chat(@Valid @RequestBody AgentChatRequest request) {
        // 不记录消息正文，避免用户输入的手机号、地址或凭证进入日志。
        // 日志中的 requestId 仍然可以关联本次请求的模型调用和工具调用。
        log.info("收到Agent会话请求 messageLength={}", request.message().length());
        AgentChatResponse response = agentService.chat(request.conversationId(), request.message());
        return ApiResponse.success(response);
    }
}
