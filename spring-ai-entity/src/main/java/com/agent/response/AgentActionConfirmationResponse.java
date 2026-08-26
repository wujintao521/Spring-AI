package com.agent.response;

import com.agent.entity.AgentPendingAction;
import com.agent.enums.AgentActionEnum;
import com.agent.enums.PendingActionStatusEnum;

import java.time.LocalDateTime;

/**
 * Agent待执行动作确认结果。
 *
 * @param actionId    待确认动作唯一编号
 * @param actionType  已确认的业务动作类型
 * @param orderNo     动作涉及的订单编号
 * @param status      动作确认后的状态
 * @param confirmedAt 用户确认时间
 */
public record AgentActionConfirmationResponse(String actionId, AgentActionEnum actionType, String orderNo, PendingActionStatusEnum status, LocalDateTime confirmedAt) {

    /**
     * 将待确认动作转换为确认响应。
     *
     * @param pendingAction 已确认的动作
     * @return 动作确认结果
     */
    public static AgentActionConfirmationResponse from(AgentPendingAction pendingAction) {
        if (pendingAction == null) {
            throw new IllegalArgumentException("待确认动作不能为空");
        }

        return new AgentActionConfirmationResponse(
                pendingAction.getActionId(),
                pendingAction.getActionType(),
                pendingAction.getOrderNo(),
                pendingAction.getStatus(),
                pendingAction.getConfirmedAt()
        );
    }
}