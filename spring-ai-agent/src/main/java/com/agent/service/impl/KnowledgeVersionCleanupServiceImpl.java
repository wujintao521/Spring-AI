package com.agent.service.impl;

import com.agent.dao.KnowledgeDocumentMapper;
import com.agent.dao.KnowledgeVersionCleanupMapper;
import com.agent.entity.KnowledgeDocument;
import com.agent.entity.KnowledgeVersionCleanup;
import com.agent.enums.KnowledgeCleanupStatusEnum;
import com.agent.exception.BusinessException;
import com.agent.service.KnowledgeVersionCleanupService;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 知识旧版本清理服务实现。
 */
@Service
public class KnowledgeVersionCleanupServiceImpl implements KnowledgeVersionCleanupService {

    /**
     * 清理任务数据访问组件。
     */
    private final KnowledgeVersionCleanupMapper knowledgeVersionCleanupMapper;

    /**
     * 知识文档数据访问组件。
     */
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;

    /**
     * 单次清理执行权的有效分钟数，当前为教学配置。
     */
    private static final long CLEANUP_LEASE_MINUTES = 5;

    /**
     * 用于精确删除旧版本知识片段。
     */
    private final VectorStore vectorStore;

    /**
     * 注入清理任务和知识文档Mapper。
     */
    public KnowledgeVersionCleanupServiceImpl(KnowledgeVersionCleanupMapper knowledgeVersionCleanupMapper,
                                              KnowledgeDocumentMapper knowledgeDocumentMapper,
                                              VectorStore vectorStore) {
        this.knowledgeVersionCleanupMapper = knowledgeVersionCleanupMapper;
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.vectorStore = vectorStore;
    }

    /**
     * 只检查当前任务能否清理，不执行状态更新或向量删除。
     */
    @Override
    public KnowledgeVersionCleanup validateBeforeCleanup(Long taskId) {
        if (taskId == null || taskId <= 0) {
            throw new BusinessException("清理任务编号必须为正整数");
        }

        KnowledgeVersionCleanup task = knowledgeVersionCleanupMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("清理任务不存在");
        }

        KnowledgeCleanupStatusEnum status = task.getStatus();
        if (status != KnowledgeCleanupStatusEnum.PENDING && status != KnowledgeCleanupStatusEnum.FAILED) {
            throw new BusinessException("当前任务状态不允许发起清理");
        }

        LocalDateTime now = LocalDateTime.now();
        if (task.getEligibleAt() == null || now.isBefore(task.getEligibleAt())) {
            throw new BusinessException("旧版本尚未到达允许清理的时间");
        }

        // 失败任务必须先安排重试时间，才能再次执行。
        if (status == KnowledgeCleanupStatusEnum.FAILED) {
            if (task.getNextRetryAt() == null) {
                throw new BusinessException("清理任务尚未安排重试");
            }
            if (now.isBefore(task.getNextRetryAt())) {
                throw new BusinessException("清理任务尚未到达重试时间");
            }
        }

        if (task.getDocumentId() == null || task.getVersion() == null || task.getVersion().isBlank()) {
            throw new BusinessException("清理任务的文档或版本信息不完整");
        }

        KnowledgeDocument document = knowledgeDocumentMapper.selectById(task.getDocumentId());
        if (document == null) {
            throw new BusinessException("知识文档管理记录不存在，不能确认清理范围");
        }

        if (document.getSource() == null || document.getSource().isBlank()
                || document.getCategory() == null || document.getCategory().isBlank()) {
            throw new BusinessException("知识文档来源或分类不完整，不能执行清理");
        }

        // 当前流程只清理被新版本替换的旧版本，异常状态下先停止删除。
        if (document.getActiveVersion() == null || document.getActiveVersion().isBlank()) {
            throw new BusinessException("知识文档没有生效版本，需核实后再清理");
        }

        if (task.getVersion().equals(document.getActiveVersion())) {
            throw new BusinessException("不能清理当前生效的知识版本");
        }

        return task;
    }


    /**
     * 领取任务并记录本次执行标识和有效期，不执行向量删除。
     */
    @Override
    public KnowledgeVersionCleanup claimTask(Long taskId) {
        KnowledgeVersionCleanup task = validateBeforeCleanup(taskId);
        LocalDateTime now = LocalDateTime.now();

        task.setExecutionToken(UUID.randomUUID().toString());
        task.setLeaseExpiresAt(now.plusMinutes(CLEANUP_LEASE_MINUTES));

        int affectedRows = knowledgeVersionCleanupMapper.tryStart(task, now);
        if (affectedRows != 1) {
            throw new BusinessException("清理任务领取失败，任务状态或生效版本已发生变化");
        }
        // 重新读取数据库，返回实际状态、执行编号及重试次数。
        return getTask(taskId);
    }

    /**
     * 查询任务，不修改任务状态。
     */
    @Override
    public KnowledgeVersionCleanup getTask(Long taskId) {
        if (taskId == null || taskId <= 0) {
            throw new BusinessException("清理任务编号必须为正整数");
        }

        KnowledgeVersionCleanup task = knowledgeVersionCleanupMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("清理任务不存在");
        }
        return task;
    }

    /**
     * 清理指定旧版本，并记录结果。
     * 不使用跨越Qdrant网络调用的MySQL长事务。
     */
    @Override
    public KnowledgeVersionCleanup executeTask(Long taskId, String executionToken) {
        if (executionToken == null || executionToken.isBlank()) {
            throw new BusinessException("执行编号不能为空");
        }

        KnowledgeVersionCleanup task = getTask(taskId);
        if (!executionToken.equals(task.getExecutionToken())) {
            throw new BusinessException("执行编号不匹配，请使用本次领取返回的编号");
        }

        // 同一次已完成的执行再次提交，直接返回已有结果。
        if (task.getStatus() == KnowledgeCleanupStatusEnum.COMPLETED) {
            return task;
        }

        if (task.getStatus() != KnowledgeCleanupStatusEnum.PROCESSING) {
            throw new BusinessException("任务未领取、正在执行或已经失败，不能直接执行");
        }

        LocalDateTime now = LocalDateTime.now();
        if (task.getLeaseExpiresAt() == null || !now.isBefore(task.getLeaseExpiresAt())) {
            throw new BusinessException("本次领取已过期，不能开始清理");
        }

        if (task.getDocumentId() == null || task.getVersion() == null || task.getVersion().isBlank()) {
            throw new BusinessException("清理任务的文档或版本信息不完整");
        }

        KnowledgeDocument document = knowledgeDocumentMapper.selectById(task.getDocumentId());
        if (document == null) {
            throw new BusinessException("知识文档管理记录不存在");
        }
        if (document.getSource() == null || document.getSource().isBlank()
                || document.getCategory() == null || document.getCategory().isBlank()) {
            throw new BusinessException("知识来源或分类为空，不能执行删除");
        }
        if (document.getActiveVersion() == null || document.getActiveVersion().isBlank()) {
            throw new BusinessException("无法确认当前生效版本，停止清理");
        }
        if (task.getVersion().equals(document.getActiveVersion())) {
            throw new BusinessException("不能删除当前生效版本");
        }

        // 删除条件来自数据库，不接受调用方随意指定来源或版本。
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        var deleteFilter = builder.and(
                builder.eq("source", document.getSource()),
                builder.and(
                        builder.eq("category", document.getCategory()),
                        builder.eq("version", task.getVersion())
                )
        ).build();

        int startedRows = knowledgeVersionCleanupMapper.beginExecution(task, LocalDateTime.now());
        if (startedRows != 1) {
            throw new BusinessException("无法开始清理，任务已执行、领取已过期或版本状态已变化");
        }

        try {
            // 只删除指定旧版本的片段，不删除collection、原始文件或MySQL任务记录。
            vectorStore.delete(deleteFilter);
        } catch (RuntimeException exception) {
            LocalDateTime failedAt = LocalDateTime.now();

            // 不把底层异常原文直接返回，避免泄露连接等内部信息。
            String error = "向量删除未确认成功：" + exception.getClass().getSimpleName();
            int failedRows;
            try {
                failedRows = knowledgeVersionCleanupMapper.markFailed(
                        taskId, executionToken, failedAt, failedAt.plusMinutes(1), error);
            } catch (RuntimeException persistenceException) {
                throw new BusinessException("清理异常且状态登记失败，请核查任务，不要直接重置状态");
            }

            if (failedRows != 1) {
                throw new BusinessException("清理异常且任务执行归属已变化，请查询任务状态");
            }
            throw new BusinessException("向量删除未确认成功，已登记失败，可在一分钟后重新领取");
        }

        // 注意：完成状态回写单独处理，不能误判成向量删除失败。
        int completedRows;
        try {
            completedRows = knowledgeVersionCleanupMapper.markCompleted(
                    taskId, executionToken, LocalDateTime.now());
        } catch (RuntimeException exception) {
            throw new BusinessException("向量删除已返回成功，但完成状态登记失败，请核查任务");
        }

        if (completedRows != 1) {
            throw new BusinessException("向量删除已返回成功，但任务执行归属已变化，请查询任务状态");
        }
        return getTask(taskId);
    }
}