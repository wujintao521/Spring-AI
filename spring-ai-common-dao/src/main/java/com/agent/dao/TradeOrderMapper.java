package com.agent.dao;

import com.agent.entity.TradeOrder;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 交易订单数据访问接口。
 *
 * 负责定义trade_order表的数据库操作。
 * 接口的具体SQL实现在TradeOrderMapper.xml中。
 */
public interface TradeOrderMapper {
    /**
     * 根据订单编号查询订单。
     *
     * 订单编号具有唯一性，因此最多返回一条记录。
     * 如果订单不存在，MyBatis会返回null。
     *
     * @param orderNo 业务订单编号
     * @return 查询到的订单；不存在时返回null
     */
    TradeOrder selectByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 根据客户姓名查询订单列表。
     *
     * 查询结果按照订单创建时间倒序排列，
     * 最近创建的订单排在最前面。
     *
     * @param customerName 客户姓名
     * @return 客户订单列表；没有订单时返回空集合
     */
    List<TradeOrder> selectListByCustomerName(@Param("customerName") String customerName);
}
