package com.agent.request;

import jakarta.validation.constraints.NotBlank;

public record OrderCancellationWorkflowRequest(
        @NotBlank(message = "订单编号不能为空")
        String orderNo
) {
}