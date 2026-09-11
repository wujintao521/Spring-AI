package com.agent.controller;

import com.agent.response.ApiResponse;
import com.agent.response.OrderKnowledgeAnswerResponse;
import com.agent.response.OrderKnowledgeResponse;
import com.agent.service.OrderKnowledgeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单知识库管理接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/order-knowledge")
public class OrderKnowledgeController {

    private final OrderKnowledgeService orderKnowledgeService;

    /**
     * 注入订单知识库服务。
     */
    public OrderKnowledgeController(OrderKnowledgeService orderKnowledgeService) {
        this.orderKnowledgeService = orderKnowledgeService;
    }

    /**
     * 导入演示用订单业务知识。
     *
     * @return 本次成功提交的知识文档数量
     */
    @PostMapping("/import-demo")
    public ApiResponse<Integer> importDemoKnowledge() {
        log.info("开始导入订单演示知识");
        int count = orderKnowledgeService.importDemoKnowledge();
        log.info("订单演示知识导入完成，文档数量={}", count);
        return ApiResponse.success(count);
    }

    /**
     * 在指定分类中检索与问题相关的订单知识。
     *
     * @param question 用户问题
     * @param category 知识分类，默认查询订单取消规则
     * @return 命中的知识文档
     */
    @GetMapping("/search")
    public ApiResponse<List<OrderKnowledgeResponse>> searchKnowledge(
            @RequestParam("question") String question,
            @RequestParam(value = "category", defaultValue = "ORDER_CANCELLATION_FILE") String category) {
        List<Document> documents = orderKnowledgeService.searchKnowledge(question, category);
        List<OrderKnowledgeResponse> results = documents.stream().map(document -> {
            String title = String.valueOf(document.getMetadata().getOrDefault("title", ""));
            return new OrderKnowledgeResponse(document.getId(), title, document.getText(), document.getScore());
        }).toList();
        return ApiResponse.success(results);
    }

    /**
     * 根据知识库回答问题，并返回参考片段。
     *
     * @param question 用户问题
     * @return 统一格式的回答及参考片段
     */
    @GetMapping("/ask")
    public ApiResponse<OrderKnowledgeAnswerResponse> answerQuestion(@RequestParam("question") String question) {
        OrderKnowledgeAnswerResponse response = orderKnowledgeService.answerQuestion(question);
        return ApiResponse.success(response);
    }

    /**
     * 从项目内的文本文件导入订单知识。
     *
     * @return 本次成功提交的知识片段数量
     */
    @PostMapping("/import-file")
    public ApiResponse<Integer> importFileKnowledge() {
        int count = orderKnowledgeService.importFileKnowledge();
        log.info("订单知识文件导入完成，片段数量={}", count);
        return ApiResponse.success(count);
    }

    /**
     * 删除订单规则文件对应的知识片段。
     *
     * @return 统一格式的操作结果
     */
    @DeleteMapping("/file")
    public ApiResponse<String> deleteFileKnowledge() {
        orderKnowledgeService.deleteFileKnowledge();
        log.info("订单规则文件对应的知识片段删除操作完成");
        return ApiResponse.success("文件知识删除操作完成");
    }

}