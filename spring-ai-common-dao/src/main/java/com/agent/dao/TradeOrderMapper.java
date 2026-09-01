package com.agent.dao;

import com.agent.entity.TradeOrder;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
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

    /**
     * 将仍允许取消的订单原子更新为已取消。
     *
     * 只有待付款或已付款状态的订单才能更新成功，
     * 防止订单已经发货后仍被并发取消。
     *
     * @param orderNo 业务订单编号
     * @param updatedAt 订单更新时间
     * @return 受影响的记录数；1表示取消成功，0表示订单不存在或状态不允许取消
     */
    int cancelCancellableOrder(@Param("orderNo") String orderNo, @Param("updatedAt") LocalDateTime updatedAt);
}
