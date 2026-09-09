package com.agent.response;

/**
 * 订单知识检索结果。
 *
 * @param id 知识文档编号
 * @param title 知识标题
 * @param content 知识原文
 * @param score 向量相似度分数，不代表答案正确率
 */
public record OrderKnowledgeResponse(String id, String title, String content, Double score) {
}