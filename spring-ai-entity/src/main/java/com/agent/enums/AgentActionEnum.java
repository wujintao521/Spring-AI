package com.agent.enums;

/**
 * Agent建议系统执行的业务动作。
 *
 * 动作只表示等待后端进一步处理的业务操作，
 * 不能仅凭大模型输出直接修改业务数据。
 */
public enum AgentActionEnum {
    /**
     * 本轮对话不需要执行写操作。
     */
    NONE("无需执行操作"),

    /**
     * 用户希望取消指定订单。
     */
    CANCEL_ORDER("取消订单");

    private final String description;

    AgentActionEnum(String description) {
        this.description = description;
    }

    /**
     * 获取业务动作说明。
     *
     * @return 业务动作说明
     */
    public String getDescription() {
        return description;
    }
}
