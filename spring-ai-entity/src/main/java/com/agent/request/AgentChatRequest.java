package com.agent.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AgentChatRequest(

        @NotBlank(message = "会话ID不能为空")
        @Size(max = 36, message = "会话ID长度不能超过36个字符")
        String conversationId,

        @NotBlank(message = "消息不能为空")
        @Size(max = 2000, message = "消息长度不能超过2000个字符")
        String message) {
}
