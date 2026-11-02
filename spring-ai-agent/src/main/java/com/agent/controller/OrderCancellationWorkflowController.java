package com.agent.controller;

import com.agent.request.OrderCancellationWorkflowRequest;
import com.agent.response.ApiResponse;
import com.agent.response.OrderCancellationWorkflowResponse;
import com.agent.service.OrderCancellationWorkflowService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workflows/order-cancellation")
public class OrderCancellationWorkflowController {

    private final OrderCancellationWorkflowService workflowService;

    public OrderCancellationWorkflowController(
            OrderCancellationWorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping("/check")
    public ApiResponse<OrderCancellationWorkflowResponse> check(
            @Valid @RequestBody OrderCancellationWorkflowRequest request) {
        return ApiResponse.success(
                workflowService.check(request.orderNo()));
    }
}