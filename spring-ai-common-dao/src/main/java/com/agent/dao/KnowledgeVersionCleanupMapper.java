package com.agent.dao;

import com.agent.entity.KnowledgeVersionCleanup;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
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

    /**
     * 尝试领取已经到期的清理任务，并再次检查目标不是生效版本。
     *
     * @param task 通过前置检查的任务
     * @param now 本次领取时间
     * @return 1表示领取成功，0表示条件已不满足
     */
    int tryStart(@Param("task") KnowledgeVersionCleanup task, @Param("now") LocalDateTime now);

    /**
     * 将本次有效领取原子更新为执行中，阻止重复调用删除接口。
     */
    int beginExecution(@Param("task") KnowledgeVersionCleanup task, @Param("now") LocalDateTime now);

    /**
     * 仅允许当前执行者登记清理完成。
     */
    int markCompleted(@Param("taskId") Long taskId, @Param("executionToken") String executionToken,
                      @Param("now") LocalDateTime now);

    /**
     * 仅允许当前执行者登记失败，并安排下次重试时间。
     */
    int markFailed(@Param("taskId") Long taskId, @Param("executionToken") String executionToken,
                   @Param("now") LocalDateTime now, @Param("nextRetryAt") LocalDateTime nextRetryAt,
                   @Param("error") String error);
}
