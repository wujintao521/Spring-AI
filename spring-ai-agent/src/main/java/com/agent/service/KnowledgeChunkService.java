package com.agent.service;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * 知识文本分块服务。
 */
public interface KnowledgeChunkService {

    /**
     * 将文本拆分为知识片段，仅在本地处理，不写入向量库。
     *
     * @param text 待拆分的文本
     * @param chunkSize 每个片段的目标 Token 数量
     * @return 拆分后的知识片段
     */
    List<Document> splitText(String text, int chunkSize);
}