package com.agent.service.impl;

import com.agent.dao.TradeOrderMapper;
import com.agent.entity.TradeOrder;
import com.agent.service.TradeOrderService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 交易订单业务服务实现类。
 *
 * 负责订单查询参数校验，并调用Mapper访问数据库。
 */
@Service
public class TradeOrderServiceImpl implements TradeOrderService {

    private final TradeOrderMapper tradeOrderMapper;

    /**
     * 通过构造方法注入交易订单Mapper。
     *
     * @param tradeOrderMapper 交易订单数据访问接口
     */
    public TradeOrderServiceImpl(TradeOrderMapper tradeOrderMapper) {
        this.tradeOrderMapper = tradeOrderMapper;
    }

    /**
     * 根据订单编号查询订单。
     *
     * @param orderNo 业务订单编号
     * @return 包含订单的Optional；订单不存在时返回空Optional
     */
    @Override
    public Optional<TradeOrder> getByOrderNo(String orderNo) {
        if (orderNo == null || orderNo.isBlank()) {
            throw new IllegalArgumentException("订单编号不能为空");
        }
        TradeOrder tradeOrder = tradeOrderMapper.selectByOrderNo(orderNo.strip());
        return Optional.ofNullable(tradeOrder);
    }

    /**
     * 根据客户姓名查询订单列表。
     *
     * @param customerName 客户姓名
     * @return 客户订单列表
     */
    @Override
    public List<TradeOrder> listByCustomerName(String customerName) {
        if (customerName == null || customerName.isBlank()) {
            throw new IllegalArgumentException("客户姓名不能为空");
        }
        return tradeOrderMapper.selectListByCustomerName(customerName.strip());
    }
}
