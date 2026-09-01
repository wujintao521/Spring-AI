package com.agent.controller;

import com.agent.entity.TradeOrder;
import com.agent.response.ApiResponse;
import com.agent.service.TradeOrderService;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 交易订单HTTP接口。
 *
 */
@RestController
@RequestMapping("/api/orders")
@Slf4j
public class TradeOrderController {

    private final TradeOrderService tradeOrderService;

    public TradeOrderController(TradeOrderService tradeOrderService) {
        this.tradeOrderService = tradeOrderService;
    }

    /**
     * 根据订单编号查询订单。
     *
     * @param orderNo 业务订单编号
     * @return 统一格式的订单查询结果
     */
    @GetMapping("/{orderNo}")
    public ApiResponse<TradeOrder> getByOrderNo(@PathVariable String orderNo) {
        log.info("根据订单编号查询订单，请求报文={}", orderNo);
        return tradeOrderService.getByOrderNo(orderNo)
                .map(ApiResponse::success)
                .orElseGet(() -> ApiResponse.failure(40401, "订单不存在"));
    }

    /**
     * 根据客户姓名查询订单列表。
     *
     * @param customerName 客户姓名
     * @return 统一格式的订单列表
     */
    @GetMapping
    public ApiResponse<List<TradeOrder>> listByCustomerName(@RequestParam String customerName) {
        log.info("根据客户姓名查询订单列表，请求报文={}", customerName);
        List<TradeOrder> tradeOrders = tradeOrderService.listByCustomerName(customerName);
        return ApiResponse.success(tradeOrders);
    }

}
