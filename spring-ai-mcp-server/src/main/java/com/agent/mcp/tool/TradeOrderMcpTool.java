package com.agent.mcp.tool;

import com.agent.entity.TradeOrder;
import com.agent.mcp.service.McpTradeOrderQueryService;
import io.modelcontextprotocol.common.McpTransportContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class TradeOrderMcpTool {

    private static final Logger log =
            LoggerFactory.getLogger(TradeOrderMcpTool.class);

    private final McpTradeOrderQueryService queryService;

    public TradeOrderMcpTool(McpTradeOrderQueryService queryService) {
        this.queryService = queryService;
    }

    @McpTool(
            name = "queryTradeOrderByOrderNo",
            description = """
                    根据订单编号查询当前用户有权访问的订单详情、状态和物流单号。
                    只能使用用户明确提供或已确认的会话上下文中的真实订单编号。
                    不得猜测、编造或使用示例订单编号。
                    未查询到可访问订单时，不得推断该订单是否属于其他用户。
                    """
    )
    public String queryTradeOrderByOrderNo(
            @McpToolParam(
                    description = "用户明确提供或已确认的真实订单编号。",
                    required = true
            ) String orderNo,
            McpTransportContext context) {

        long userId = requireUserId(context);

        log.info("MCP订单编号查询工具被调用，userId={}", userId);

        return queryService.getByOrderNo(orderNo, userId)
                .map(this::formatOrder)
                .orElse("未查询到可访问的订单。");
    }

    @McpTool(
            name = "queryTradeOrdersByCustomerName",
            description = """
                    根据客户姓名，在当前用户有权访问的订单范围内查询订单。
                    当用户未提供订单编号，但提供了客户姓名并要求查询订单时使用。
                    姓名只能来自用户明确提供或已确认的会话上下文，不得猜测。
                    客户姓名只是筛选条件，不能用于切换用户身份。
                    """
    )
    public String queryTradeOrdersByCustomerName(
            @McpToolParam(
                    description = "用户明确提供或已确认的真实客户姓名。",
                    required = true
            ) String customerName,
            McpTransportContext context) {

        long userId = requireUserId(context);

        log.info("MCP客户姓名查询工具被调用，userId={}", userId);

        // 查询层返回订单对象集合。
        List<TradeOrder> orders = queryService.listByCustomerName(customerName, userId);

        if (orders.isEmpty()) {
            return "未查询到可访问的订单。";
        }

        // 工具层负责将订单对象转换为模型可读文本。
        return orders.stream()
                .map(this::formatOrder)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    /**
     * 私有订单查询必须具有有效的最终用户身份。
     */
    private long requireUserId(McpTransportContext context) {
        Object value = context == null ? null : context.get("userId");

        // 当前 McpTransportConfig 将用户编号保存为 Long。
        if (!(value instanceof Long userId) || userId <= 0) {
            throw new IllegalStateException(
                    "缺少有效用户身份，拒绝查询订单。");
        }

        return userId;
    }

    private String formatOrder(TradeOrder order) {
        String status = order.getStatus() == null
                ? "UNKNOWN（未知）"
                : "%s（%s）".formatted(
                order.getStatus().name(),
                order.getStatus().getDescription());

        String trackingNo = order.getTrackingNo();
        if (trackingNo == null || trackingNo.isBlank()) {
            trackingNo = "暂无";
        }

        return """
                订单编号：%s；客户姓名：%s；商品名称：%s；购买数量：%s；订单金额：%s元；订单状态：%s；物流单号：%s。
                """.formatted(
                order.getOrderNo(),
                order.getCustomerName(),
                order.getProductName(),
                order.getQuantity(),
                order.getTotalAmount(),
                status,
                trackingNo
        ).strip();
    }
}