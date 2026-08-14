package com.agent.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AgentChatRequest(
        @NotBlank(message = "消息不能为空")
        @Size(max = 2000, message = "消息长度不能超过2000个字符")
        String message) {
}
