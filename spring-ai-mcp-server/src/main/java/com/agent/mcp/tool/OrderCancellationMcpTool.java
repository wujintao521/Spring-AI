package com.agent.mcp.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class OrderCancellationMcpTool {

    private static final Logger log =
            LoggerFactory.getLogger(OrderCancellationMcpTool.class);

    /**
     * 根据订单状态判断取消资格。
     * 保持与原有本地工具相同的业务规则，不执行订单修改。
     */
    @McpTool(
            name = "checkTradeOrderCancellationEligibility",
            description = """
                    根据订单当前状态判断是否允许取消以及是否需要退款。
                    当用户询问具体订单是否可以取消时，
                    必须先调用queryTradeOrderByOrderNo获得真实订单状态，
                    再调用本工具。
                    本工具只提供资格判断，不会取消订单或执行退款。
                    """
    )
    public String checkTradeOrderCancellationEligibility(
            @McpToolParam(
                    description = """
                            订单查询工具返回的真实订单状态。
                            只能是PENDING_PAYMENT、PAID、SHIPPED、COMPLETED或CANCELLED。
                            不得根据用户描述猜测。
                            """,
                    required = true
            ) String orderStatus) {

        if (orderStatus == null || orderStatus.isBlank()) {
            throw new IllegalArgumentException("订单状态不能为空");
        }

        String normalizedStatus = orderStatus.strip();

        log.info("MCP取消资格判断工具被调用，orderStatus={}", normalizedStatus);

        return switch (normalizedStatus) {
            case "PENDING_PAYMENT" ->
                    "是否允许取消：是；是否需要退款：否；原因：订单尚未付款，可以直接取消。";
            case "PAID" ->
                    "是否允许取消：是；是否需要退款：是；原因：订单已经付款但尚未发货，取消后需要进入退款流程。";
            case "SHIPPED" ->
                    "是否允许取消：否；是否需要退款：否；原因：订单已经发货，不能直接取消，需要申请售后退货。";
            case "COMPLETED" ->
                    "是否允许取消：否；是否需要退款：否；原因：订单已经完成，不能执行订单取消，需要根据售后规则处理。";
            case "CANCELLED" ->
                    "是否允许取消：否；是否需要退款：否；原因：订单已经处于取消状态，不需要重复取消。";
            default ->
                    throw new IllegalArgumentException("不支持的订单状态");
        };
    }
}