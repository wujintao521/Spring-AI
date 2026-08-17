package com.agent.tool;

import com.agent.enums.OrderStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 为Agent提供交易订单取消资格判断能力。
 *
 * 本工具只根据订单状态判断是否允许取消，
 * 不执行任何数据库写入操作。
 */
@Component
@Slf4j
public class TradeOrderCancellationTool {
    /**
     * 根据订单状态判断订单是否允许取消。
     *
     * @param orderStatus 订单当前状态
     * @return 订单取消资格判断结果
     */
    @Tool(
            name = "checkTradeOrderCancellationEligibility",
            description = "根据订单当前状态判断是否允许取消以及是否需要退款。当用户询问某个具体订单是否可以取消时，必须先调用queryTradeOrderByOrderNo查询订单并获得真实状态，然后再调用本工具。本工具只进行资格判断，不会修改订单。"
    )
    public String checkTradeOrderCancellationEligibility(
            @ToolParam(description = "订单状态枚举，只能是PENDING_PAYMENT、PAID、SHIPPED、COMPLETED或CANCELLED")
            OrderStatusEnum orderStatus) {
        if (orderStatus == null) {
            throw new IllegalArgumentException("订单状态不能为空");
        }
        log.info("Agent调用订单取消资格判断工具，orderStatus={}", orderStatus);
        return switch (orderStatus) {
            case PENDING_PAYMENT -> "是否允许取消：是；是否需要退款：否；原因：订单尚未付款，可以直接取消。";
            case PAID -> "是否允许取消：是；是否需要退款：是；原因：订单已经付款但尚未发货，取消后需要进入退款流程。";
            case SHIPPED -> "是否允许取消：否；是否需要退款：否；原因：订单已经发货，不能直接取消，需要申请售后退货。";
            case COMPLETED -> "是否允许取消：否；是否需要退款：否；原因：订单已经完成，不能执行订单取消，需要根据售后规则处理。";
            case CANCELLED -> "是否允许取消：否；是否需要退款：否；原因：订单已经处于取消状态，不需要重复取消。";
        };
    }
}
