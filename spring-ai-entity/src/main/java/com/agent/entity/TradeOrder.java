package com.agent.entity;

import com.agent.enums.OrderStatusEnum;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class TradeOrder {

    /**
     * 订单主键。
     */
    private Long id;

    /**
     * 业务订单编号。
     *
     * 用于在业务系统中唯一标识一笔订单。
     */
    private String orderNo;

    /**
     * 客户姓名。
     */
    private String customerName;

    /**
     * 商品名称。
     */
    private String productName;

    /**
     * 商品购买数量。
     */
    private Integer quantity;

    /**
     * 订单总金额。
     *
     * 使用BigDecimal保存金额，避免浮点数计算产生精度误差。
     */
    private BigDecimal totalAmount;

    /**
     * 订单当前状态。
     */
    private OrderStatusEnum status;

    /**
     * 物流单号。
     *
     * 订单尚未发货时，该字段可以为空。
     */
    private String trackingNo;

    /**
     * 订单创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 订单最后更新时间。
     */
    private LocalDateTime updatedAt;

    /**
     * 订单所属用户ID。
     * 用于判断当前用户是否有权访问该订单，不能由大模型自行决定。
     */
    private Long userId;
}
