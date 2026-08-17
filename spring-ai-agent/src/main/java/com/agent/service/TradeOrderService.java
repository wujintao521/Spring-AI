package com.agent.service;

import com.agent.entity.TradeOrder;

import java.util.List;
import java.util.Optional;

/**
 * 交易订单业务服务。
 *
 * 定义交易订单相关的业务操作，
 * 具体实现由impl包中的实现类完成。
 */
public interface TradeOrderService {

    /**
     * 根据订单编号查询订单。
     *
     * @param orderNo 业务订单编号
     * @return 包含订单的Optional；订单不存在时返回空Optional
     */
    Optional<TradeOrder> getByOrderNo(String orderNo);

    /**
     * 根据客户姓名查询订单列表。
     *
     * @param customerName 客户姓名
     * @return 客户订单列表
     */
    List<TradeOrder> listByCustomerName(String customerName);
}
