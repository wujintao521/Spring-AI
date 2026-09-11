package com.agent.dao;

import com.agent.entity.KnowledgeDocument;
import org.apache.ibatis.annotations.Param;

/**
 * 知识文档数据访问接口，负责管理文档的生效版本。
 */
public interface KnowledgeDocumentMapper {

    /**
     * 新增尚未发布的知识文档，生效版本为空，初始修订次数为零。
     *
     * @param document 知识文档
     * @return 受影响的记录数；来源和分类重复时由数据库唯一约束拒绝
     */
    int insert(KnowledgeDocument document);

    /**
     * 根据来源和分类查询知识文档。
     *
     * @param source 知识来源标识
     * @param category 知识分类
     * @return 知识文档；不存在时返回null
     */
    KnowledgeDocument selectBySourceAndCategory(@Param("source") String source, @Param("category") String category);

    /**
     * 仅在修订次数未变化时切换生效版本，同时递增修订次数。
     * 调用前必须确保新版本的全部片段已经成功写入向量库。
     *
     * @param document 包含主键、新版本号和新版本片段数量的文档
     * @param expectedRevision 开始本次导入时读取的修订次数
     * @return 1表示切换成功，0表示记录不存在或已被其他请求更新
     */
    int switchActiveVersion(@Param("document") KnowledgeDocument document, @Param("expectedRevision") Long expectedRevision);
}
