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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Agent已确认动作执行服务单元测试。
 *
 * 使用Mockito模拟Mapper，不访问真实数据库。
 */
class AgentActionExecutionServiceImplTest {

    private static final String ACTION_ID = "11111111-1111-4111-8111-111111111111";
    private static final String ORDER_NO = "ORD202608170001";

    private AgentPendingActionMapper agentPendingActionMapper;
    private TradeOrderMapper tradeOrderMapper;
    private AgentActionExecutionServiceImpl executionService;

    /**
     * 每个测试执行前重新创建模拟对象和被测试服务。
     */
    @BeforeEach
    void setUp() {
        agentPendingActionMapper = mock(AgentPendingActionMapper.class);
        tradeOrderMapper = mock(TradeOrderMapper.class);
        executionService = new AgentActionExecutionServiceImpl(agentPendingActionMapper, tradeOrderMapper);
    }

    /**
     * 验证已确认且未过期的动作能够成功执行。
     */
    @Test
    void shouldExecuteConfirmedActionSuccessfully() {
        AgentPendingAction confirmedAction = createPendingAction(PendingActionStatusEnum.CONFIRMED);
        LocalDateTime executedAt = LocalDateTime.now();

        AgentPendingAction executedAction = createPendingAction(PendingActionStatusEnum.EXECUTED);
        executedAction.setExecutedAt(executedAt);

        TradeOrder cancelledOrder = createTradeOrder(OrderStatusEnum.CANCELLED);

        when(agentPendingActionMapper.selectByActionIdForUpdate(ACTION_ID)).thenReturn(confirmedAction);
        when(tradeOrderMapper.cancelCancellableOrder(eq(ORDER_NO), any(LocalDateTime.class))).thenReturn(1);
        when(agentPendingActionMapper.markCancelOrderActionExecuted(eq(ACTION_ID), any(LocalDateTime.class))).thenReturn(1);
        when(agentPendingActionMapper.selectByActionId(ACTION_ID)).thenReturn(executedAction);
        when(tradeOrderMapper.selectByOrderNo(ORDER_NO)).thenReturn(cancelledOrder);

        AgentActionExecutionResponse response = executionService.executeConfirmedAction(ACTION_ID);

        assertEquals(ACTION_ID, response.actionId());
        assertEquals(ORDER_NO, response.orderNo());
        assertEquals(PendingActionStatusEnum.EXECUTED, response.actionStatus());
        assertEquals(OrderStatusEnum.CANCELLED, response.orderStatus());
        assertFalse(response.alreadyExecuted());

        verify(tradeOrderMapper).cancelCancellableOrder(eq(ORDER_NO), any(LocalDateTime.class));
        verify(agentPendingActionMapper).markCancelOrderActionExecuted(eq(ACTION_ID), any(LocalDateTime.class));
    }

    /**
     * 验证已执行动作再次请求时不会重复修改订单。
     */
    @Test
    void shouldReturnExistingResultWhenActionAlreadyExecuted() {
        LocalDateTime executedAt = LocalDateTime.now();

        AgentPendingAction executedAction = createPendingAction(PendingActionStatusEnum.EXECUTED);
        executedAction.setExecutedAt(executedAt);

        TradeOrder cancelledOrder = createTradeOrder(OrderStatusEnum.CANCELLED);

        when(agentPendingActionMapper.selectByActionIdForUpdate(ACTION_ID)).thenReturn(executedAction);
        when(tradeOrderMapper.selectByOrderNo(ORDER_NO)).thenReturn(cancelledOrder);

        AgentActionExecutionResponse response = executionService.executeConfirmedAction(ACTION_ID);
        System.out.println("幂等执行结果：" + response);
        assertTrue(response.alreadyExecuted());
        assertEquals(executedAt, response.executedAt());
        assertEquals(OrderStatusEnum.CANCELLED, response.orderStatus());

        verify(tradeOrderMapper, never()).cancelCancellableOrder(eq(ORDER_NO), any(LocalDateTime.class));
        verify(agentPendingActionMapper, never()).markCancelOrderActionExecuted(eq(ACTION_ID), any(LocalDateTime.class));
    }

    /**
     * 验证未确认动作不能执行，也不能修改订单。
     */
    @Test
    void shouldRejectActionWhenActionIsNotConfirmed() {
        AgentPendingAction pendingAction = createPendingAction(PendingActionStatusEnum.PENDING);

        when(agentPendingActionMapper.selectByActionIdForUpdate(ACTION_ID)).thenReturn(pendingAction);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> executionService.executeConfirmedAction(ACTION_ID)
        );

        assertEquals("待执行动作尚未确认或当前状态不允许执行", exception.getMessage());

        verifyNoInteractions(tradeOrderMapper);
        verify(agentPendingActionMapper, never()).markCancelOrderActionExecuted(eq(ACTION_ID), any(LocalDateTime.class));
    }

    /**
     * 创建测试使用的待执行动作。
     */
    private AgentPendingAction createPendingAction(PendingActionStatusEnum status) {
        AgentPendingAction pendingAction = new AgentPendingAction();
        pendingAction.setActionId(ACTION_ID);
        pendingAction.setConversationId("test-conversation");
        pendingAction.setActionType(AgentActionEnum.CANCEL_ORDER);
        pendingAction.setOrderNo(ORDER_NO);
        pendingAction.setStatus(status);
        pendingAction.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        return pendingAction;
    }

    /**
     * 创建测试使用的订单。
     */
    private TradeOrder createTradeOrder(OrderStatusEnum status) {
        TradeOrder tradeOrder = new TradeOrder();
        tradeOrder.setOrderNo(ORDER_NO);
        tradeOrder.setStatus(status);
        return tradeOrder;
    }
}