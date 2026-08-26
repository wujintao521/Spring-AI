package com.agent.controller;

import com.agent.request.AgentActionConfirmationRequest;
import com.agent.response.AgentActionConfirmationResponse;
import com.agent.response.ApiResponse;
import com.agent.service.AgentPendingActionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agent待执行动作接口。
 */
@RestController
@RequestMapping("/api/agent/actions")
@Slf4j
public class AgentActionController {

    private final AgentPendingActionService agentPendingActionService;

    public AgentActionController(AgentPendingActionService agentPendingActionService) {
        this.agentPendingActionService = agentPendingActionService;
    }

    /**
     * 确认一条尚未过期的待执行动作。
     *
     * @param request 动作确认请求
     * @return 动作确认结果
     */
    @PostMapping("/confirm")
    public ApiResponse<AgentActionConfirmationResponse> confirm(
            @Valid @RequestBody AgentActionConfirmationRequest request) {
        log.info("用户确认Agent待执行动作，actionId={}", request.actionId());
        return agentPendingActionService
                .confirmPendingAction(request.actionId())
                .map(AgentActionConfirmationResponse::from)
                .map(ApiResponse::success)
                .orElseGet(() -> ApiResponse.failure(
                        40901,
                        "待确认动作无效、已处理或已过期"
                ));
    }
}