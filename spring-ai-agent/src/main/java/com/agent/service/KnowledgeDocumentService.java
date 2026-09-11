package com.agent.service;

import com.agent.entity.KnowledgeDocument;

/**
 * 知识文档管理服务。
 */
public interface KnowledgeDocumentService {

    /**
     * 查询知识文档，不存在时创建尚未发布的管理记录。
     *
     * @param source 知识来源标识
     * @param category 知识分类
     * @return 知识文档管理信息
     */
    KnowledgeDocument getOrCreate(String source, String category);

    /**
     * 发布已经完整写入向量库的新版本。
     *
     * @param original 开始本次导入时读取的文档信息
     * @param newVersion 本次导入生成的新版本号
     * @param chunkCount 新版本的片段数量
     */
    void publishVersion(KnowledgeDocument original, String newVersion, int chunkCount);
}