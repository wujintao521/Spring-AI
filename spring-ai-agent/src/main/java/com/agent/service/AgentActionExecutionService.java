package com.agent.service;

import com.agent.response.AgentActionExecutionResponse;

/**
 * Agent已确认业务动作执行服务。
 */
public interface AgentActionExecutionService {

    /**
     * 执行一条已经获得用户确认的业务动作。
     *
     * @param actionId 待执行动作唯一编号
     * @return 动作执行结果
     */
    AgentActionExecutionResponse executeConfirmedAction(String actionId);
}