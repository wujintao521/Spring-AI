package com.agent.response;

import com.agent.entity.AgentPendingAction;
import com.agent.enums.AgentActionEnum;
import com.agent.enums.PendingActionStatusEnum;

import java.time.LocalDateTime;

/**
 * Agent待执行动作放弃结果。
 *
 * @param actionId   待确认动作唯一编号
 * @param actionType 被放弃的业务动作类型
 * @param orderNo    动作涉及的订单编号
 * @param status     动作放弃后的状态
 * @param rejectedAt 用户放弃时间
 */
public record AgentActionRejectionResponse(
        String actionId,
        AgentActionEnum actionType,
        String orderNo,
        PendingActionStatusEnum status,
        LocalDateTime rejectedAt
) {

    /**
     * 将待确认动作转换为放弃响应。
     */
    public static AgentActionRejectionResponse from(AgentPendingAction pendingAction) {
        if (pendingAction == null) {
            throw new IllegalArgumentException("待确认动作不能为空");
        }

        return new AgentActionRejectionResponse(
                pendingAction.getActionId(),
                pendingAction.getActionType(),
                pendingAction.getOrderNo(),
                pendingAction.getStatus(),
                pendingAction.getUpdatedAt()
        );
    }
}