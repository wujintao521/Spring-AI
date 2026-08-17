package com.agent.controller;

import com.agent.request.AgentChatRequest;
import com.agent.response.AgentChatResponse;
import com.agent.response.ApiResponse;
import com.agent.service.AgentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agent HTTP 接口。
 */
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/chat")
    public ApiResponse<String> chat(@Valid @RequestBody AgentChatRequest request) {
        String answer = agentService.chat(request.conversationId(), request.message());
        return ApiResponse.success(answer);
    }
}
