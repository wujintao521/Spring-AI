package com.agent.response;

import com.agent.entity.AgentPendingAction;
import com.agent.entity.TradeOrder;
import com.agent.enums.AgentActionEnum;
import com.agent.enums.OrderStatusEnum;
import com.agent.enums.PendingActionStatusEnum;

import java.time.LocalDateTime;

/**
 * Agent业务动作执行结果。
 *
 * @param actionId       动作唯一编号
 * @param actionType     实际执行的业务动作
 * @param orderNo        动作涉及的订单编号
 * @param actionStatus   待执行动作最终状态
 * @param orderStatus    订单执行后的状态
 * @param executedAt     动作执行完成时间
 * @param alreadyExecuted 本次请求到达前动作是否已经执行完成
 */
public record AgentActionExecutionResponse(String actionId, AgentActionEnum actionType, String orderNo, PendingActionStatusEnum actionStatus, OrderStatusEnum orderStatus, LocalDateTime executedAt, boolean alreadyExecuted) {

    /**
     * 根据动作和订单生成执行响应。
     *
     * @param pendingAction 已执行的业务动作
     * @param tradeOrder 执行后的订单
     * @param alreadyExecuted 本次请求到达前是否已经执行
     * @return 动作执行结果
     */
    public static AgentActionExecutionResponse from(AgentPendingAction pendingAction, TradeOrder tradeOrder, boolean alreadyExecuted) {
        if (pendingAction == null) {
            throw new IllegalArgumentException("已执行动作不能为空");
        }
        if (tradeOrder == null) {
            throw new IllegalArgumentException("执行后的订单不能为空");
        }

        return new AgentActionExecutionResponse(
                pendingAction.getActionId(),
                pendingAction.getActionType(),
                pendingAction.getOrderNo(),
                pendingAction.getStatus(),
                tradeOrder.getStatus(),
                pendingAction.getExecutedAt(),
                alreadyExecuted
        );
    }
}