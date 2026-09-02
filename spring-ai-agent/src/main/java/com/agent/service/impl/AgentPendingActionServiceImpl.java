package com.agent.service.impl;

import com.agent.dao.AgentPendingActionMapper;
import com.agent.entity.AgentPendingAction;
import com.agent.enums.AgentActionEnum;
import com.agent.enums.BusinessErrorCodeEnum;
import com.agent.enums.PendingActionStatusEnum;
import com.agent.exception.BusinessException;
import com.agent.service.AgentPendingActionService;
import org.springframework.http.HttpStatus;
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

    public AgentPendingActionServiceImpl(AgentPendingActionMapper agentPendingActionMapper) {
        this.agentPendingActionMapper = agentPendingActionMapper;
    }

    /**
     * 创建一条等待用户确认的动作。
     */
    @Override
    public AgentPendingAction createPendingAction(String conversationId, AgentActionEnum actionType, String orderNo) {
        if (conversationId == null || conversationId.isBlank()) {
            throw new BusinessException("待执行动作编号不能为空");
        }

        if (actionType == null || actionType == AgentActionEnum.NONE) {
            throw new BusinessException("待执行动作不存在");
        }

        if (orderNo == null || !ORDER_NO_PATTERN.matcher(orderNo).matches()) {
            throw new BusinessException("订单编号格式不正确");
        }

        LocalDateTime now = LocalDateTime.now();

        AgentPendingAction pendingAction = new AgentPendingAction();
        pendingAction.setActionId(UUID.randomUUID().toString());
        pendingAction.setConversationId(conversationId);
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
     * 根据动作编号查询待确认动作。
     */
    @Override
    public Optional<AgentPendingAction> getByActionId(String actionId) {
        if (actionId == null || actionId.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(agentPendingActionMapper.selectByActionId(actionId));
    }

    /**
     * 原子确认一条尚未过期的待确认动作。
     */
    @Override
    public Optional<AgentPendingAction> confirmPendingAction(String actionId) {
        if (actionId == null || actionId.isBlank()) {
            throw new BusinessException("待确认动作编号不能为空");
        }
        LocalDateTime confirmedAt = LocalDateTime.now();
        int affectedRows = agentPendingActionMapper.confirmPendingAction(actionId, confirmedAt);
        if (affectedRows != 1) {
            return Optional.empty();
        }
        return Optional.ofNullable(agentPendingActionMapper.selectByActionId(actionId));
    }

    /**
     * 原子放弃一条尚未过期的待确认动作。
     */
    @Override
    public Optional<AgentPendingAction> rejectPendingAction(String actionId) {
        if (actionId == null || actionId.isBlank()) {
            throw new BusinessException("待确认动作编号不能为空");
        }
        LocalDateTime rejectedAt = LocalDateTime.now();
        int affectedRows = agentPendingActionMapper.rejectPendingAction(actionId, rejectedAt);
        if (affectedRows != 1) {
            return Optional.empty();
        }
        return Optional.ofNullable(agentPendingActionMapper.selectByActionId(actionId));
    }
}