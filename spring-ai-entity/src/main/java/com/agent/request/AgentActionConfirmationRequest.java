package com.agent.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 用户确认Agent待执行动作的请求。
 *
 * @param actionId 待确认动作唯一编号
 */
public record AgentActionConfirmationRequest(

        @NotBlank(message = "待确认动作编号不能为空")
        @Pattern(
                regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$",
                message = "待确认动作编号格式不正确"
        )
        String actionId
) {
}