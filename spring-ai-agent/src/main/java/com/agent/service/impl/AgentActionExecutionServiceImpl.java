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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Agent已确认业务动作执行服务实现。
 *
 * 负责在同一个数据库事务中完成订单取消和动作状态更新。
 */
@Service
public class AgentActionExecutionServiceImpl implements AgentActionExecutionService {

    private final AgentPendingActionMapper agentPendingActionMapper;
    private final TradeOrderMapper tradeOrderMapper;

    public AgentActionExecutionServiceImpl(AgentPendingActionMapper agentPendingActionMapper, TradeOrderMapper tradeOrderMapper) {
        this.agentPendingActionMapper = agentPendingActionMapper;
        this.tradeOrderMapper = tradeOrderMapper;
    }

    /**
     * 执行已经获得用户确认的取消订单动作。
     *
     * 事务中的执行顺序：
     * 1. 锁定待执行动作；
     * 2. 校验动作状态和类型；
     * 3. 条件更新订单状态；
     * 4. 将动作更新为已执行；
     * 5. 返回最终执行结果。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentActionExecutionResponse executeConfirmedAction(String actionId) {
        if (actionId == null || actionId.isBlank()) {
            throw new BusinessException("待执行动作编号不能为空");
        }

        AgentPendingAction pendingAction = agentPendingActionMapper.selectByActionIdForUpdate(actionId.strip());
        if (pendingAction == null) {
            throw new BusinessException("待执行动作不存在");
        }

        validateActionType(pendingAction);

        if (pendingAction.getStatus() == PendingActionStatusEnum.EXECUTED) {
            return buildAlreadyExecutedResponse(pendingAction);
        }

        if (pendingAction.getStatus() != PendingActionStatusEnum.CONFIRMED) {
            throw new BusinessException("待执行动作尚未确认或当前状态不允许执行");
        }

        LocalDateTime executedAt = LocalDateTime.now();
        if (pendingAction.getExpiresAt() == null || !pendingAction.getExpiresAt().isAfter(executedAt)) {
            throw new BusinessException("待执行动作已经过期");
        }

        int cancelledRows = tradeOrderMapper.cancelCancellableOrder(pendingAction.getOrderNo(), executedAt);
        if (cancelledRows != 1) {
            throw new IllegalStateException("订单不存在或当前状态不允许取消");
        }

        int executedRows = agentPendingActionMapper.markCancelOrderActionExecuted(pendingAction.getActionId(), executedAt);
        if (executedRows != 1) {
            throw new IllegalStateException("更新待执行动作状态失败");
        }

        AgentPendingAction executedAction = agentPendingActionMapper.selectByActionId(pendingAction.getActionId());
        TradeOrder cancelledOrder = tradeOrderMapper.selectByOrderNo(pendingAction.getOrderNo());

        if (executedAction == null || cancelledOrder == null) {
            throw new IllegalStateException("查询动作执行结果失败");
        }

        return AgentActionExecutionResponse.from(executedAction, cancelledOrder, false);
    }

    /**
     * 校验当前执行器是否支持该动作。
     */
    private void validateActionType(AgentPendingAction pendingAction) {
        if (pendingAction.getActionType() != AgentActionEnum.CANCEL_ORDER) {
            throw new IllegalStateException("当前执行器不支持该业务动作");
        }

        if (pendingAction.getOrderNo() == null || pendingAction.getOrderNo().isBlank()) {
            throw new IllegalStateException("待执行动作没有关联订单编号");
        }
    }

    /**
     * 返回已经执行完成的动作结果，不重复修改数据库。
     */
    private AgentActionExecutionResponse buildAlreadyExecutedResponse(AgentPendingAction pendingAction) {
        TradeOrder tradeOrder = tradeOrderMapper.selectByOrderNo(pendingAction.getOrderNo());

        if (tradeOrder == null) {
            throw new IllegalStateException("已执行动作关联的订单不存在");
        }

        if (tradeOrder.getStatus() != OrderStatusEnum.CANCELLED) {
            throw new IllegalStateException("动作执行记录与订单状态不一致");
        }

        return AgentActionExecutionResponse.from(pendingAction, tradeOrder, true);
    }
}