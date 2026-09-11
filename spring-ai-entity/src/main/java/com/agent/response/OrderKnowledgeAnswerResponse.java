package com.agent.response;

import java.util.List;

/**
 * 知识库问答响应。
 *
 * @param answer 回答内容
 * @param references 本轮提供给模型的参考片段，不代表全部被实际引用
 */
public record OrderKnowledgeAnswerResponse(String answer, List<OrderKnowledgeReference> references) {
}