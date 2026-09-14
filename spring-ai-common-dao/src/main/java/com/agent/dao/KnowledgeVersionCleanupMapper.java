package com.agent.dao;

import com.agent.entity.KnowledgeVersionCleanup;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 知识旧版本清理任务数据访问接口。
 */
public interface KnowledgeVersionCleanupMapper {

    /**
     * 登记旧版本清理任务，初始状态为待处理。
     *
     * @param cleanup 包含文档编号、旧版本号和最早清理时间的任务
     * @return 受影响的记录数；重复任务由唯一约束拒绝
     */
    int insert(KnowledgeVersionCleanup cleanup);

    /**
     * 根据任务主键查询清理任务。
     *
     * @param id 清理任务主键
     * @return 清理任务；不存在时返回null
     */
    KnowledgeVersionCleanup selectById(@Param("id") Long id);

    /**
     * 查询指定文档最近的清理任务，最多返回五十条。
     *
     * @param documentId 知识文档编号
     * @return 按任务主键倒序排列的清理记录
     */
    List<KnowledgeVersionCleanup> selectRecentByDocumentId(@Param("documentId") Long documentId);
}
