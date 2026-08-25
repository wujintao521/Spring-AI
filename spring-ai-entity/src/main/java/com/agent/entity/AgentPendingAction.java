package com.agent.entity;

import com.agent.enums.AgentActionEnum;
import com.agent.enums.PendingActionStatusEnum;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Agent等待用户确认的业务动作。
 *
 * 用于保存大模型提出、但尚未获得用户确认的写操作。
 */
@Getter
@Setter
@NoArgsConstructor
public class AgentPendingAction {

    /**
     * 待确认动作主键。
     */
    private Long id;

    /**
     * 对外使用的待确认动作唯一编号。
     *
     * 前端确认或者放弃动作时，需要携带该编号。
     */
    private String actionId;

    /**
     * 产生该动作的会话编号。
     */
    private String conversationId;

    /**
     * 等待执行的业务动作类型。
     */
    private AgentActionEnum actionType;

    /**
     * 该动作涉及的订单编号。
     */
    private String orderNo;

    /**
     * 待确认动作当前处理状态。
     */
    private PendingActionStatusEnum status;

    /**
     * 用户确认该动作的截止时间。
     *
     * 超过该时间后，动作不能继续确认或者执行。
     */
    private LocalDateTime expiresAt;

    /**
     * 用户确认动作的时间。
     *
     * 尚未确认时可以为空。
     */
    private LocalDateTime confirmedAt;

    /**
     * 业务动作实际执行完成的时间。
     *
     * 尚未执行时可以为空。
     */
    private LocalDateTime executedAt;

    /**
     * 待确认动作创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 待确认动作最后更新时间。
     */
    private LocalDateTime updatedAt;
}