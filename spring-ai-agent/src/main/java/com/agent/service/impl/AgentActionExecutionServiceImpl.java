package com.agent.service.impl;

import com.agent.dao.AgentPendingActionMapper;
import com.agent.dao.TradeOrderMapper;
import com.agent.entity.AgentPendingAction;
import com.agent.entity.TradeOrder;
import com.agent.enums.AgentActionEnum;
import com.agent.enums.OrderStatusEnum;
import com.agent.enums.PendingActionStatusEnum;
import com.agent.exception.BusinessException;
import com.agent.response.AgentActionExecutionResponse;
import com.agent.service.AgentActionExecutionService;
import com.agent.service.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 在同一个事务中完成订单取消和动作状态更新。
 * 查询、修改、重复执行返回结果，均限定当前用户归属。
 */
@Service
public class AgentActionExecutionServiceImpl implements AgentActionExecutionService {

    private final AgentPendingActionMapper agentPendingActionMapper;
    private final TradeOrderMapper tradeOrderMapper;
    private final CurrentUserService currentUserService;

    public AgentActionExecutionServiceImpl(AgentPendingActionMapper agentPendingActionMapper,
                                           TradeOrderMapper tradeOrderMapper, CurrentUserService currentUserService) {
        this.agentPendingActionMapper = agentPendingActionMapper;
        this.tradeOrderMapper = tradeOrderMapper;
        this.currentUserService = currentUserService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentActionExecutionResponse executeConfirmedAction(String actionId) {
        Long userId = currentUserService.requireUserId();

        if (actionId == null || actionId.isBlank()) {
            throw new BusinessException("待执行动作编号不能为空");
        }

        // 在事务中锁定当前用户拥有的动作，防止同一动作被并发执行。
        AgentPendingAction pendingAction = agentPendingActionMapper
                .selectByActionIdForUpdate(actionId.strip(), userId);

        if (pendingAction == null || !userId.equals(pendingAction.getUserId())) {
            throw new BusinessException("待执行动作不存在或无权访问");
        }

        validateActionType(pendingAction);

        // 必须在归属校验之后处理幂等返回，避免向其他用户泄露执行结果。
        if (pendingAction.getStatus() == PendingActionStatusEnum.EXECUTED) {
            return buildAlreadyExecutedResponse(pendingAction, userId);
        }

        if (pendingAction.getStatus() != PendingActionStatusEnum.CONFIRMED) {
            throw new BusinessException("待执行动作尚未确认或当前状态不允许执行");
        }

        LocalDateTime executedAt = LocalDateTime.now();
        if (pendingAction.getExpiresAt() == null || !pendingAction.getExpiresAt().isAfter(executedAt)) {
            throw new BusinessException("待执行动作已经过期");
        }

        // 动作属于当前用户，不代表订单归属可以省略检查。
        // 更新时同时检查订单归属与状态。
        int cancelledRows = tradeOrderMapper
                .cancelCancellableOrder(pendingAction.getOrderNo(), userId, executedAt);
        if (cancelledRows != 1) {
            throw new BusinessException("订单不存在、无权访问或当前状态不允许取消");
        }

        int executedRows = agentPendingActionMapper
                .markCancelOrderActionExecuted(pendingAction.getActionId(), userId, executedAt);
        if (executedRows != 1) {
            // 抛出异常，使本事务中的订单取消一并回滚。
            throw new IllegalStateException("更新待执行动作状态失败");
        }

        AgentPendingAction executedAction = agentPendingActionMapper
                .selectByActionIdAndUserId(pendingAction.getActionId(), userId);
        TradeOrder cancelledOrder = tradeOrderMapper
                .selectByOrderNoAndUserId(pendingAction.getOrderNo(), userId);

        if (executedAction == null || cancelledOrder == null) {
            throw new IllegalStateException("查询动作执行结果失败");
        }

        return AgentActionExecutionResponse.from(executedAction, cancelledOrder, false);
    }

    private void validateActionType(AgentPendingAction pendingAction) {
        if (pendingAction.getActionType() != AgentActionEnum.CANCEL_ORDER) {
            throw new IllegalStateException("当前执行器不支持该业务动作");
        }

        if (pendingAction.getOrderNo() == null || pendingAction.getOrderNo().isBlank()) {
            throw new IllegalStateException("待执行动作没有关联订单编号");
        }
    }

    /**
     * 已执行动作可以重复查询结果，但仍需限定关联订单的用户归属。
     */
    private AgentActionExecutionResponse buildAlreadyExecutedResponse(AgentPendingAction pendingAction, Long userId) {
        TradeOrder tradeOrder = tradeOrderMapper
                .selectByOrderNoAndUserId(pendingAction.getOrderNo(), userId);

        if (tradeOrder == null) {
            throw new BusinessException("订单不存在或无权访问");
        }

        if (tradeOrder.getStatus() != OrderStatusEnum.CANCELLED) {
            throw new IllegalStateException("动作执行记录与订单状态不一致");
        }

        return AgentActionExecutionResponse.from(pendingAction, tradeOrder, true);
    }
}