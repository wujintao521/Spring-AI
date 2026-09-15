package com.agent.enums;

/**
 * 知识旧版本清理任务状态。
 */
public enum KnowledgeCleanupStatusEnum {

    /**
     * 等待清理，尚未开始执行。
     */
    PENDING,

    /**
     * 已领取，等待开始执行清理。
     */
    PROCESSING,

    /**
     * 已开始执行向量清理。
     */
    EXECUTING,

    /**
     * 清理已完成。
     */
    COMPLETED,

    /**
     * 本次清理失败，等待后续重试。
     */
    FAILED


}