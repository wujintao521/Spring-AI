package com.agent.controller;

import com.agent.exception.BusinessException;
import com.agent.request.AgentActionConfirmationRequest;
import com.agent.request.AgentActionExecutionRequest;
import com.agent.request.AgentActionRejectionRequest;
import com.agent.response.AgentActionConfirmationResponse;
import com.agent.response.AgentActionExecutionResponse;
import com.agent.response.AgentActionRejectionResponse;
import com.agent.response.ApiResponse;
import com.agent.service.AgentActionExecutionService;
import com.agent.service.AgentPendingActionService;
import com.alibaba.fastjson2.JSONObject;
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
    private final AgentActionExecutionService agentActionExecutionService;

    public AgentActionController(AgentPendingActionService agentPendingActionService,AgentActionExecutionService agentActionExecutionService) {
        this.agentPendingActionService = agentPendingActionService;
        this.agentActionExecutionService = agentActionExecutionService;
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
        log.info("用户确认Agent待执行动作，请求报文={}", JSONObject.toJSONString(request));
        return agentPendingActionService
                .confirmPendingAction(request.actionId())
                .map(AgentActionConfirmationResponse::from)
                .map(ApiResponse::success)
                .orElseThrow(() -> new BusinessException("待确认动作无效、已处理或已过期"));
    }

    /**
     * 放弃一条尚未过期的待执行动作。
     *
     * @param request 动作放弃请求
     * @return 动作放弃结果
     */
    @PostMapping("/reject")
    public ApiResponse<AgentActionRejectionResponse> reject(
            @Valid @RequestBody AgentActionRejectionRequest request) {
        log.info("用户放弃Agent待执行动作，actionId={}", request.actionId());
        log.info("用户放弃Agent待执行动作，请求报文={}", JSONObject.toJSONString(request));
        return agentPendingActionService
                .rejectPendingAction(request.actionId())
                .map(AgentActionRejectionResponse::from)
                .map(ApiResponse::success)
                .orElseThrow(() -> new BusinessException("待确认动作无效、已处理或已过期"));
    }

    /**
     * 执行一条已经获得用户确认的业务动作。
     *
     * @param request 动作执行请求
     * @return 动作执行结果
     */
    @PostMapping("/execute")
    public ApiResponse<AgentActionExecutionResponse> execute(
            @Valid @RequestBody AgentActionExecutionRequest request) {
        log.info("执行已确认的Agent业务动作，actionId={}", request.actionId());
        AgentActionExecutionResponse response = agentActionExecutionService.executeConfirmedAction(request.actionId());
        return ApiResponse.success(response);
    }

}