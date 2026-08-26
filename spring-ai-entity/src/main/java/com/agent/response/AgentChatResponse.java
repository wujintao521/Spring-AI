package com.agent.response;

import com.agent.enums.AgentActionEnum;
import com.agent.enums.AgentIntentEnum;

import java.util.List;

/**
 * Agent聊天接口返回给调用方的数据。
 *
 * 该对象由Java后端根据经过校验的大模型结果创建，
 * 不直接作为大模型结构化输出的接收对象。
 *
 * @param answer               展示给用户的自然语言回答
 * @param intent               本轮识别出的用户业务意图
 * @param relatedOrderNos      本轮对话涉及的订单编号
 * @param pendingAction        等待用户确认的业务动作
 * @param confirmationRequired 执行待确认动作前是否需要用户确认
 * @param actionId             后端生成的待确认动作唯一编号
 */
public record AgentChatResponse(String answer, AgentIntentEnum intent, List<String> relatedOrderNos, AgentActionEnum pendingAction, boolean confirmationRequired, String actionId) {

    /**
     * 将经过校验的大模型结果转换为HTTP响应数据。
     *
     * @param modelResponse 经过校验的大模型结构化结果
     * @return Agent聊天接口响应数据
     */
    public static AgentChatResponse from(AgentModelResponse modelResponse, String actionId) {
        if (modelResponse == null) {
            throw new IllegalArgumentException("大模型结构化结果不能为空");
        }
        boolean hasPendingAction = modelResponse.pendingAction() != AgentActionEnum.NONE;
        if (hasPendingAction && (actionId == null || actionId.isBlank())) {
            throw new IllegalArgumentException("存在待确认动作时actionId不能为空");
        }
        if (!hasPendingAction && actionId != null && !actionId.isBlank()) {
            throw new IllegalArgumentException("没有待确认动作时不能返回actionId");
        }
        return new AgentChatResponse(
                modelResponse.answer(),
                modelResponse.intent(),
                List.copyOf(modelResponse.relatedOrderNos()),
                modelResponse.pendingAction(),
                modelResponse.confirmationRequired(),
                hasPendingAction ? actionId.trim() : null
        );
    }
}