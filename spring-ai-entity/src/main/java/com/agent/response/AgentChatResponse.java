package com.agent.response;

import com.agent.enums.AgentIntentEnum;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Agent返回给调用方的结构化聊天结果。
 *
 * @param answer          展示给用户的自然语言回答
 * @param intent          本轮识别出的用户业务意图
 * @param relatedOrderNos 本轮对话涉及的订单编号
 */
public record AgentChatResponse(String answer, AgentIntentEnum intent, List<String> relatedOrderNos) {

    /**
     * 当前系统使用的订单编号格式。
     */
    private static final Pattern ORDER_NO_PATTERN = Pattern.compile("^ORD\\d{12}$");

    /**
     * 校验并规范化大模型生成的结构化结果。
     */
    public AgentChatResponse {
        if (answer == null || answer.isBlank()) {
            throw new IllegalArgumentException("Agent回答不能为空");
        }

        if (intent == null) {
            throw new IllegalArgumentException("Agent业务意图不能为空");
        }

        answer = answer.trim();

        relatedOrderNos = relatedOrderNos == null
                ? List.of()
                : relatedOrderNos.stream()
                .filter(orderNo -> orderNo != null && !orderNo.isBlank())
                .map(String::trim)
                .distinct()
                .toList();

        boolean containsInvalidOrderNo = relatedOrderNos.stream()
                .anyMatch(orderNo -> !ORDER_NO_PATTERN.matcher(orderNo).matches());

        if (containsInvalidOrderNo) {
            throw new IllegalArgumentException("Agent返回了格式不正确的订单编号");
        }
    }
}