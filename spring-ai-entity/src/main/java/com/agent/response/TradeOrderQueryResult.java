package com.agent.response;

/**
 * 供Java工作流读取的订单查询结果。
 *
 * @param found 是否查到当前用户有权访问的订单
 * @param orderNo 查询的订单编号；未查到时仅回显查询条件
 * @param status 查到订单时为真实状态；未查到时为UNKNOWN
 */
public record TradeOrderQueryResult(
        boolean found,
        String orderNo,
        String status
) {
}