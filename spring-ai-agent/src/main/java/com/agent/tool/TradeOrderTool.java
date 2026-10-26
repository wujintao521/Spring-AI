package com.agent.tool;

import com.agent.entity.TradeOrder;
import com.agent.service.TradeOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 为 Agent 提供交易订单查询能力。
 *
 * 大模型不会直接访问数据库，而是调用此工具，
 * 再由工具通过业务服务查询订单数据。
 */
@Component
public class TradeOrderTool {
    private static final Logger log = LoggerFactory.getLogger(TradeOrderTool.class);

    private final TradeOrderService tradeOrderService;

    /**
     * 通过构造方法注入交易订单业务服务。
     *
     * @param tradeOrderService 交易订单业务服务
     */
    public TradeOrderTool(TradeOrderService tradeOrderService) {
        this.tradeOrderService = tradeOrderService;
    }

    /**
     * 根据订单编号查询订单详情。
     *
     * @param orderNo 业务订单编号
     * @return 适合大模型理解的订单信息
     */
    @Tool(
            name = "queryTradeOrderByOrderNo",
            description = "根据订单编号查询订单详情、订单状态和物流单号。当用户提供了明确的订单编号并询问订单信息时，必须调用此工具。"
    )
    public String queryTradeOrderByOrderNo(@ToolParam(description = "@ToolParam(description = \"需要查询的真实业务订单编号。只能使用用户明确提供或已确认的会话上下文中的订单编号。不得猜测、编造或使用示例值；无法确定时应先询问用户，不调用本工具。\")") String orderNo) {
        log.info("Agent调用订单编号查询工具");
        return tradeOrderService.getByOrderNo(orderNo)
                .map(this::formatTradeOrder)
                .orElse("没有查询到订单编号为%s的订单。".formatted(orderNo));
    }

    /**
     * 根据客户姓名查询订单列表。
     *
     * @param customerName 客户姓名
     * @return 该客户的订单信息
     */
    @Tool(
            name = "queryTradeOrdersByCustomerName",
            description = "根据客户姓名查询该客户的订单列表。当用户没有提供订单编号，但是提供了客户姓名并要求查询订单时，调用此工具。"
    )
    public String queryTradeOrdersByCustomerName(@ToolParam(description = "需要查询的真实客户姓名。只能使用用户明确提供或已确认的会话上下文中的姓名。不得猜测、编造或使用示例值；无法确定时应先询问用户，不调用本工具。") String customerName) {
        log.info("Agent调用客户姓名查询工具");
        List<TradeOrder> tradeOrders = tradeOrderService.listByCustomerName(customerName);
        if (tradeOrders.isEmpty()) {
            return "没有查询到客户%s的订单。".formatted(customerName);
        }
        return tradeOrders.stream().map(this::formatTradeOrder).collect(Collectors.joining(System.lineSeparator()));
    }

    /**
     * 将订单对象转换成便于大模型理解的文本。
     *
     * @param tradeOrder 交易订单
     * @return 格式化后的订单信息
     */
    private String formatTradeOrder(TradeOrder tradeOrder) {
        String status = tradeOrder.getStatus() == null ? "UNKNOWN（未知）" : "%s（%s）".formatted(tradeOrder.getStatus().name(), tradeOrder.getStatus().getDescription());
        String trackingNo = tradeOrder.getTrackingNo() == null || tradeOrder.getTrackingNo().isBlank() ? "暂无" : tradeOrder.getTrackingNo();
        return "订单编号：%s；客户姓名：%s；商品名称：%s；购买数量：%s；订单金额：%s元；订单状态：%s；物流单号：%s。".formatted(
                tradeOrder.getOrderNo(),
                tradeOrder.getCustomerName(),
                tradeOrder.getProductName(),
                tradeOrder.getQuantity(),
                tradeOrder.getTotalAmount(),
                status,
                trackingNo
        );
    }
}
