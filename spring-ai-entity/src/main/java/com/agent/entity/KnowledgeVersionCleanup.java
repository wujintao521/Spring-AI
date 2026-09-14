package com.agent.entity;

import com.agent.enums.KnowledgeCleanupStatusEnum;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 知识旧版本清理任务。
 * 每条记录指定一份知识文档中需要清理的一个旧版本。
 */
@Getter
@Setter
@NoArgsConstructor
public class KnowledgeVersionCleanup {

    /**
     * 清理任务主键。
     */
    private Long id;

    /**
     * 所属知识文档的主键。
     */
    private Long documentId;

    /**
     * 本次任务需要清理的旧版本号。
     */
    private String version;

    /**
     * 清理任务当前状态。
     */
    private KnowledgeCleanupStatusEnum status;

    /**
     * 最早允许执行清理的时间，为旧版本查询预留完成时间。
     */
    private LocalDateTime eligibleAt;

    /**
     * 失败后的重试次数。
     */
    private Integer retryCount;

    /**
     * 下次允许重试的时间，尚未安排重试时为空。
     */
    private LocalDateTime nextRetryAt;

    /**
     * 最近一次清理失败的原因。
     */
    private String lastError;

    /**
     * 清理完成时间，尚未完成时为空。
     */
    private LocalDateTime completedAt;

    /**
     * 清理任务创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 清理任务最后更新时间。
     */
    private LocalDateTime updatedAt;

}