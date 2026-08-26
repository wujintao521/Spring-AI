package com.agent.dao;

import com.agent.entity.AgentPendingAction;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * Agent待确认动作数据访问接口。
 *
 * 负责待确认动作的新增、查询和状态流转。
 */
public interface AgentPendingActionMapper {

    /**
     * 新增一条待确认动作。
     *
     * @param pendingAction 待确认动作
     * @return 受影响的记录数
     */
    int insert(AgentPendingAction pendingAction);

    /**
     * 根据动作唯一编号查询待确认动作。
     *
     * @param actionId 待确认动作唯一编号
     * @return 待确认动作；不存在时返回null
     */
    AgentPendingAction selectByActionId(@Param("actionId") String actionId);

    /**
     * 将有效的待确认动作原子更新为已确认。
     *
     * 只有当前状态为PENDING，并且尚未过期时才能更新成功。
     *
     * @param actionId 待确认动作唯一编号
     * @param confirmedAt 用户确认时间
     * @return 受影响的记录数；1表示确认成功，0表示动作无效
     */
    int confirmPendingAction(@Param("actionId") String actionId, @Param("confirmedAt") LocalDateTime confirmedAt);
}
