package com.agent.enums;

/**
 * Agent识别出的用户意图。
 */
public enum AgentIntentEnum {

    /**
     * 不涉及具体业务操作的普通对话。
     */
    GENERAL_CHAT("普通对话"),

    /**
     * 查询订单详情、状态或物流信息。
     */
    ORDER_QUERY("订单查询"),

    /**
     * 判断订单是否具备取消资格。
     */
    ORDER_CANCELLATION_ELIGIBILITY("订单取消资格判断");

    private final String description;

    AgentIntentEnum(String description) {
        this.description = description;
    }

    /**
     * 获取意图的中文说明。
     *
     * @return 意图说明
     */
    public String getDescription() {
        return description;
    }

}
