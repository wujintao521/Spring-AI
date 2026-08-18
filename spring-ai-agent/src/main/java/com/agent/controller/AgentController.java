package com.agent.controller;

import com.agent.request.AgentChatRequest;
import com.agent.response.AgentChatResponse;
import com.agent.response.ApiResponse;
import com.agent.service.AgentService;
import com.alibaba.fastjson2.JSONObject;
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
        log.info("会话请求报文{}", JSONObject.toJSONString(request));
        AgentChatResponse response = agentService.chat(request.conversationId(), request.message());
        return ApiResponse.success(response);
    }
}
