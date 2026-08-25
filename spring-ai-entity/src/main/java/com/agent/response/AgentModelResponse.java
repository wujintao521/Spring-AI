package com.agent.response;

import com.agent.enums.AgentActionEnum;
import com.agent.enums.AgentIntentEnum;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 大模型生成的结构化结果。
 *
 * 该对象中的所有字段都来自大模型，属于不可信数据，
 * 必须经过Java后端校验和处理后才能返回给调用方。
 *
 * @param answer              大模型生成的自然语言回答
 * @param intent              大模型识别出的用户业务意图
 * @param relatedOrderNos     大模型提取出的相关订单编号
 * @param pendingAction       大模型建议执行的待确认动作
 * @param confirmationRequired 大模型判断该动作是否需要用户确认
 */
public record AgentModelResponse(String answer, AgentIntentEnum intent, List<String> relatedOrderNos, AgentActionEnum pendingAction, boolean confirmationRequired) {

    /**
     * 当前系统使用的订单编号格式。
     */
    private static final Pattern ORDER_NO_PATTERN = Pattern.compile("^ORD\\d{12}$");

    /**
     * 校验并规范化大模型生成的结构化结果。
     */
    public AgentModelResponse {
        if (answer == null || answer.isBlank()) {
            throw new IllegalArgumentException("大模型回答不能为空");
        }

        if (intent == null) {
            throw new IllegalArgumentException("大模型业务意图不能为空");
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
            throw new IllegalArgumentException("大模型返回了格式不正确的订单编号");
        }

        if (pendingAction == null) {
            throw new IllegalArgumentException("大模型待执行动作不能为空");
        }

        if (pendingAction == AgentActionEnum.NONE && confirmationRequired) {
            throw new IllegalArgumentException("没有待执行动作时不能要求用户确认");
        }

        if (pendingAction != AgentActionEnum.NONE && !confirmationRequired) {
            throw new IllegalArgumentException("存在待执行动作时必须要求用户确认");
        }

        if (pendingAction == AgentActionEnum.CANCEL_ORDER && relatedOrderNos.size() != 1) {
            throw new IllegalArgumentException("取消订单时必须包含一个明确的订单编号");
        }
    }
}