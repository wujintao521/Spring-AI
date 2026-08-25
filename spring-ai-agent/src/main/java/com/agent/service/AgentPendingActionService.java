package com.agent.service;

import com.agent.entity.AgentPendingAction;
import com.agent.enums.AgentActionEnum;

import java.util.Optional;

/**
 * Agent待确认动作业务服务。
 */
public interface AgentPendingActionService {

    /**
     * 创建一条等待用户确认的动作。
     *
     * @param conversationId 产生动作的会话编号
     * @param actionType     待执行动作类型
     * @param orderNo        动作涉及的订单编号
     * @return 创建成功的待确认动作
     */
    AgentPendingAction createPendingAction(String conversationId, AgentActionEnum actionType, String orderNo);

    /**
     * 根据动作编号查询待确认动作。
     *
     * @param actionId 待确认动作唯一编号
     * @return 待确认动作
     */
    Optional<AgentPendingAction> getByActionId(String actionId);

    /**
     * 确认一条尚未过期的待确认动作。
     *
     * @param actionId 待确认动作唯一编号
     * @return true表示确认成功，false表示动作无效
     */
    boolean confirmPendingAction(String actionId);
}