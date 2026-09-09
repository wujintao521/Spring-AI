package com.agent.service;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * 订单知识库服务。
 */
public interface OrderKnowledgeService {

    /**
     * 导入演示用订单业务知识。
     *
     * @return 本次成功提交的知识文档数量
     */
    int importDemoKnowledge();

    /**
     * 根据问题检索相关订单知识。
     *
     * @param question 用户问题
     * @return 按相关性排序的知识文档
     */
    List<Document> searchKnowledge(String question);

    /**
     * 根据订单知识库回答问题。
     *
     * @param question 用户问题
     * @return 基于知识库生成的回答
     */
    String answerQuestion(String question);
}