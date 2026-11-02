package com.agent.response;

import java.util.List;

/**
 * 只读取消资格咨询的结果，不代表订单已取消。
 */
public record OrderCancellationWorkflowResponse(
        boolean found,
        String orderNo,
        String orderStatus,
        String eligibility,
        List<String> completedSteps
) {
}