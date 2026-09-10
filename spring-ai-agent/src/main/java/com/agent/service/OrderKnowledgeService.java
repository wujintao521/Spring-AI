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
     * 在指定分类中检索订单知识。
     *
     * @param question 用户问题
     * @param category 知识分类
     * @return 符合分类和相似度条件的知识文档
     */
    List<Document> searchKnowledge(String question, String category);

    /**
     * 根据订单知识库回答问题。
     *
     * @param question 用户问题
     * @return 基于知识库生成的回答
     */
    String answerQuestion(String question);

    /**
     * 从文本文件导入订单知识。
     *
     * @return 本次成功提交的知识片段数量
     */
    int importFileKnowledge();
}