package com.agent.response;

/**
 * 提供给模型的知识参考片段。
 *
 * @param id 片段编号
 * @param title 知识标题
 * @param source 来源文件
 * @param section 所属章节
 * @param content 知识原文
 * @param score 检索相似度分数，不代表答案正确率
 */
public record OrderKnowledgeReference(
        String id, String title, String source, String section, String content, Double score) {
}