package com.agent.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 执行已确认Agent动作的请求。
 *
 * @param actionId 待执行动作唯一编号
 */
public record AgentActionExecutionRequest(

        @NotBlank(message = "待执行动作编号不能为空")
        @Pattern(
                regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$",
                message = "待执行动作编号格式不正确"
        )
        String actionId
) {
}