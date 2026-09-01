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
     * 根据动作编号查询并锁定待执行动作。
     *
     * 该方法必须在事务中调用，用于防止同一个动作被并发执行。
     *
     * @param actionId 待执行动作唯一编号
     * @return 待执行动作；不存在时返回null
     */
    AgentPendingAction selectByActionIdForUpdate(@Param("actionId") String actionId);

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

    /**
     * 将有效的待确认动作原子更新为已放弃。
     *
     * 只有当前状态为PENDING，并且尚未过期时才能更新成功。
     *
     * @param actionId 待确认动作唯一编号
     * @param rejectedAt 用户放弃时间
     * @return 受影响的记录数；1表示放弃成功，0表示动作无效
     */
    int rejectPendingAction(@Param("actionId") String actionId, @Param("rejectedAt") LocalDateTime rejectedAt);

    /**
     * 将已确认的取消订单动作原子更新为已执行。
     *
     * @param actionId 待执行动作唯一编号
     * @param executedAt 动作执行完成时间
     * @return 受影响的记录数；1表示更新成功，0表示动作状态不允许执行
     */
    int markCancelOrderActionExecuted(@Param("actionId") String actionId, @Param("executedAt") LocalDateTime executedAt);
}
