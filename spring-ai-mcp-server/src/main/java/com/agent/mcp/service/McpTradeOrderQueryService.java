package com.agent.mcp.service;

import com.agent.dao.TradeOrderMapper;
import com.agent.entity.TradeOrder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class McpTradeOrderQueryService {

    private final TradeOrderMapper tradeOrderMapper;

    public McpTradeOrderQueryService(TradeOrderMapper tradeOrderMapper) {
        this.tradeOrderMapper = tradeOrderMapper;
    }

    /**
     * userId 由工具层从 MCP 上下文提取，不能由模型填写。
     */
    public Optional<TradeOrder> getByOrderNo(String orderNo, long userId) {
        requireValidUserId(userId);
        String normalizedOrderNo = requireText(orderNo, "订单编号");
        TradeOrder order = tradeOrderMapper.selectByOrderNoAndUserId(
                normalizedOrderNo, userId);
        return Optional.ofNullable(order);
    }

    public List<TradeOrder> listByCustomerName(
            String customerName, long userId) {
        requireValidUserId(userId);
        String normalizedName = requireText(customerName, "客户姓名");
        return tradeOrderMapper.selectListByCustomerNameAndUserId(
                normalizedName, userId);
    }

    private void requireValidUserId(long userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("用户编号无效");
        }
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        return value.strip();
    }
}