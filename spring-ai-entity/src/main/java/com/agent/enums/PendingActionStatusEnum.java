package com.agent.enums;

/**
 * Agent待确认动作的处理状态。
 *
 * 该枚举描述待确认动作从创建到结束的生命周期。
 */
public enum PendingActionStatusEnum {

    /**
     * 动作已经创建，正在等待用户确认。
     */
    PENDING("等待确认"),

    /**
     * 用户已经确认，等待执行业务操作。
     */
    CONFIRMED("已确认"),

    /**
     * 业务操作已经执行完成。
     */
    EXECUTED("已执行"),

    /**
     * 用户主动放弃该操作。
     */
    CANCELLED("已取消"),

    /**
     * 动作超过确认有效时间，不能继续执行。
     */
    EXPIRED("已过期");

    private final String description;

    PendingActionStatusEnum(String description) {
        this.description = description;
    }

    /**
     * 获取动作状态说明。
     *
     * @return 动作状态说明
     */
    public String getDescription() {
        return description;
    }
}