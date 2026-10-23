package com.agent.service.impl;

import com.agent.dao.AgentPendingActionMapper;
import com.agent.dao.TradeOrderMapper;
import com.agent.entity.AgentPendingAction;
import com.agent.enums.AgentActionEnum;
import com.agent.enums.PendingActionStatusEnum;
import com.agent.exception.BusinessException;
import com.agent.service.AgentPendingActionService;
import com.agent.service.CurrentUserService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Agent待确认动作业务服务实现。
 */
@Service
public class AgentPendingActionServiceImpl implements AgentPendingActionService {

    /**
     * 待确认动作的有效分钟数。
     */
    private static final long ACTION_EXPIRATION_MINUTES = 5;

    /**
     * 当前系统使用的订单编号格式。
     */
    private static final Pattern ORDER_NO_PATTERN = Pattern.compile("^ORD\\d{12}$");

    private final AgentPendingActionMapper agentPendingActionMapper;
    private final TradeOrderMapper tradeOrderMapper;
    private final CurrentUserService currentUserService;

    public AgentPendingActionServiceImpl(AgentPendingActionMapper agentPendingActionMapper,
                                         TradeOrderMapper tradeOrderMapper, CurrentUserService currentUserService) {
        this.agentPendingActionMapper = agentPendingActionMapper;
        this.tradeOrderMapper = tradeOrderMapper;
        this.currentUserService = currentUserService;
    }

    /**
     * 创建一条等待用户确认的动作。
     */
    @Override
    public AgentPendingAction createPendingAction(String conversationId, AgentActionEnum actionType, String orderNo) {
        // 用户身份由服务端认证结果决定。
        Long userId = currentUserService.requireUserId();

        if (conversationId == null || conversationId.isBlank()) {
            throw new BusinessException("会话ID不能为空");
        }

        if (actionType == null || actionType == AgentActionEnum.NONE) {
            throw new BusinessException("待确认动作类型不合法");
        }

        if (orderNo == null || !ORDER_NO_PATTERN.matcher(orderNo).matches()) {
            throw new BusinessException("订单编号格式不正确");
        }

        // 创建待确认动作之前，也必须检查订单归属。
        // 即使模型返回了别人的订单号，后端也不允许为它创建动作。
        if (tradeOrderMapper.selectByOrderNoAndUserId(orderNo, userId) == null) {
            throw new BusinessException("订单不存在或无权访问");
        }

        LocalDateTime now = LocalDateTime.now();

        AgentPendingAction pendingAction = new AgentPendingAction();
        pendingAction.setActionId(UUID.randomUUID().toString());
        pendingAction.setConversationId(conversationId);
        pendingAction.setUserId(userId);
        pendingAction.setActionType(actionType);
        pendingAction.setOrderNo(orderNo);
        pendingAction.setStatus(PendingActionStatusEnum.PENDING);
        pendingAction.setExpiresAt(now.plusMinutes(ACTION_EXPIRATION_MINUTES));

        int affectedRows = agentPendingActionMapper.insert(pendingAction);
        if (affectedRows != 1) {
            throw new BusinessException("创建待确认动作失败");
        }

        return pendingAction;
    }

    /**
     * 查询当前认证用户拥有的动作。
     */
    @Override
    public Optional<AgentPendingAction> getByActionId(String actionId) {
        Long userId = currentUserService.requireUserId();

        if (actionId == null || actionId.isBlank()) {
            return Optional.empty();
        }

        AgentPendingAction action = agentPendingActionMapper.selectByActionIdAndUserId(actionId.strip(), userId);
        return Optional.ofNullable(action);
    }

    /**
     * 原子确认当前认证用户的有效动作。
     */
    @Override
    public Optional<AgentPendingAction> confirmPendingAction(String actionId) {
        Long userId = currentUserService.requireUserId();

        if (actionId == null || actionId.isBlank()) {
            throw new BusinessException("待确认动作编号不能为空");
        }

        String normalizedActionId = actionId.strip();
        LocalDateTime confirmedAt = LocalDateTime.now();

        int affectedRows = agentPendingActionMapper.confirmPendingAction(normalizedActionId, userId, confirmedAt);
        if (affectedRows != 1) {
            // 包含无权访问、动作不存在、状态不允许、已过期等情况。
            return Optional.empty();
        }

        return Optional.ofNullable(agentPendingActionMapper.selectByActionIdAndUserId(normalizedActionId, userId));
    }

    /**
     * 原子放弃当前认证用户的有效动作。
     */
    @Override
    public Optional<AgentPendingAction> rejectPendingAction(String actionId) {
        Long userId = currentUserService.requireUserId();

        if (actionId == null || actionId.isBlank()) {
            throw new BusinessException("待确认动作编号不能为空");
        }

        String normalizedActionId = actionId.strip();
        LocalDateTime rejectedAt = LocalDateTime.now();

        int affectedRows = agentPendingActionMapper.rejectPendingAction(normalizedActionId, userId, rejectedAt);
        if (affectedRows != 1) {
            return Optional.empty();
        }

        return Optional.ofNullable(agentPendingActionMapper.selectByActionIdAndUserId(normalizedActionId, userId));
    }
}