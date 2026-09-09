package com.agent.controller;

import com.agent.response.ApiResponse;
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
     * 检索与问题相关的订单知识。
     *
     * @param question 用户问题
     * @return 命中的知识文档
     */
    @GetMapping("/search")
    public ApiResponse<List<OrderKnowledgeResponse>> searchKnowledge(@RequestParam("question") String question) {
        List<Document> documents = orderKnowledgeService.searchKnowledge(question);
        List<OrderKnowledgeResponse> results = documents.stream().map(document -> {
            String title = String.valueOf(document.getMetadata().getOrDefault("title", ""));
            return new OrderKnowledgeResponse(document.getId(), title, document.getText(), document.getScore());
        }).toList();
        return ApiResponse.success(results);
    }

    /**
     * 根据知识库回答订单规则问题。
     *
     * @param question 用户问题
     * @return 统一格式的知识问答结果
     */
    @GetMapping("/ask")
    public ApiResponse<String> answerQuestion(@RequestParam("question") String question) {
        String answer = orderKnowledgeService.answerQuestion(question);
        return ApiResponse.success(answer);
    }
}