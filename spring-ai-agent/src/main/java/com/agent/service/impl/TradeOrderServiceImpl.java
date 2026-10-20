package com.agent.service.impl;

import com.agent.dao.TradeOrderMapper;
import com.agent.entity.TradeOrder;
import com.agent.service.CurrentUserService;
import com.agent.service.TradeOrderService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 普通订单接口和 Agent 工具共用的订单查询服务。
 * 所有查询均限定当前认证用户的订单归属。
 */
@Service
public class TradeOrderServiceImpl implements TradeOrderService {

    private final TradeOrderMapper tradeOrderMapper;
    private final CurrentUserService currentUserService;

    public TradeOrderServiceImpl(TradeOrderMapper tradeOrderMapper, CurrentUserService currentUserService) {
        this.tradeOrderMapper = tradeOrderMapper;
        this.currentUserService = currentUserService;
    }

    @Override
    public Optional<TradeOrder> getByOrderNo(String orderNo) {
        Long userId = currentUserService.requireUserId();

        if (orderNo == null || orderNo.isBlank()) {
            throw new IllegalArgumentException("订单编号不能为空");
        }

        // 模型仍然只提供订单号，用户身份由后端认证上下文提供。
        TradeOrder tradeOrder = tradeOrderMapper.selectByOrderNoAndUserId(orderNo.strip(), userId);
        return Optional.ofNullable(tradeOrder);
    }

    @Override
    public List<TradeOrder> listByCustomerName(String customerName) {
        Long userId = currentUserService.requireUserId();

        if (customerName == null || customerName.isBlank()) {
            throw new IllegalArgumentException("客户姓名不能为空");
        }

        // 即使姓名相同，也只返回当前用户有权访问的订单。
        return tradeOrderMapper.selectListByCustomerNameAndUserId(customerName.strip(), userId);
    }
}