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
     * 只有订单归属正确，且当前状态允许取消时，才更新成功。
     */
    int cancelCancellableOrder(@Param("orderNo") String orderNo, @Param("userId") Long userId,
                               @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * 查询指定用户拥有的订单。
     *
     * userId 必须来自后端确认的用户身份，不能由大模型决定。
     * 订单不存在或不属于该用户时，均返回 null。
     */
    TradeOrder selectByOrderNoAndUserId(@Param("orderNo") String orderNo, @Param("userId") Long userId);

    /**
     * 在指定用户有权访问的订单范围内，按客户姓名查询。
     *
     * 姓名只是筛选条件，不能代替用户身份。
     */
    List<TradeOrder> selectListByCustomerNameAndUserId(@Param("customerName") String customerName, @Param("userId") Long userId);
}
