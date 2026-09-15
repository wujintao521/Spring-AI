package com.agent.service;

import com.agent.entity.KnowledgeVersionCleanup;

/**
 * 知识旧版本清理服务。
 */
public interface KnowledgeVersionCleanupService {

    /**
     * 检查清理任务当前是否满足执行条件，不修改状态、不删除数据。
     *
     * @param taskId 清理任务编号
     * @return 通过本次检查的任务，不代表已经领取执行权
     */
    KnowledgeVersionCleanup validateBeforeCleanup(Long taskId);

    /**
     * 校验并尝试领取清理任务，不执行向量删除。
     *
     * @param taskId 清理任务编号
     * @return 本次成功领取的任务
     */
    KnowledgeVersionCleanup claimTask(Long taskId);

    /**
     * 查询清理任务的当前状态。
     */
    KnowledgeVersionCleanup getTask(Long taskId);

    /**
     * 使用领取时返回的执行编号，清理任务指定的旧版本。
     */
    KnowledgeVersionCleanup executeTask(Long taskId, String executionToken);

}