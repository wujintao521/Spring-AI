package com.agent.service.impl;

import com.agent.dao.KnowledgeDocumentMapper;
import com.agent.dao.KnowledgeVersionCleanupMapper;
import com.agent.entity.KnowledgeDocument;
import com.agent.entity.KnowledgeVersionCleanup;
import com.agent.exception.BusinessException;
import com.agent.service.KnowledgeDocumentService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 知识文档管理服务实现。
 */
@Service
public class KnowledgeDocumentServiceImpl implements KnowledgeDocumentService {

    /**
     * 旧版本保留小时数，暂定为24小时，后续可以改为配置项。
     */
    private static final long OLD_VERSION_RETENTION_HOURS = 24;

    /**
     * 知识旧版本清理任务数据访问组件。
     */
    private final KnowledgeVersionCleanupMapper knowledgeVersionCleanupMapper;

    /**
     * 知识文档数据访问组件。
     */
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;

    /**
     * 通过构造方法注入知识文档Mapper。
     */
    public KnowledgeDocumentServiceImpl(KnowledgeDocumentMapper knowledgeDocumentMapper,
                                        KnowledgeVersionCleanupMapper knowledgeVersionCleanupMapper) {
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.knowledgeVersionCleanupMapper = knowledgeVersionCleanupMapper;
    }

    /**
     * 获取文档管理记录，不存在时初始化。
     */
    @Override
    public KnowledgeDocument getOrCreate(String source, String category) {
        if (source == null || source.isBlank()) {
            throw new BusinessException("知识来源不能为空");
        }
        if (category == null || category.isBlank()) {
            throw new BusinessException("知识分类不能为空");
        }

        String normalizedSource = source.strip();
        String normalizedCategory = category.strip();

        KnowledgeDocument existing = knowledgeDocumentMapper.selectBySourceAndCategory(
                normalizedSource, normalizedCategory);
        if (existing != null) {
            return existing;
        }

        KnowledgeDocument document = new KnowledgeDocument();
        document.setSource(normalizedSource);
        document.setCategory(normalizedCategory);

        try {
            knowledgeDocumentMapper.insert(document);
        } catch (DuplicateKeyException exception) {
            // 另一个请求可能已经创建了相同记录，重新读取即可。
            KnowledgeDocument concurrent = knowledgeDocumentMapper.selectBySourceAndCategory(
                    normalizedSource, normalizedCategory);
            if (concurrent == null) {
                throw exception;
            }
            return concurrent;
        }

        // 重新查询，获取数据库生成的默认值及创建时间。
        KnowledgeDocument created = knowledgeDocumentMapper.selectBySourceAndCategory(
                normalizedSource, normalizedCategory);
        if (created == null) {
            throw new BusinessException("知识文档初始化失败");
        }
        return created;
    }

    /**
     * 发布新版本，并在同一个MySQL事务中登记旧版本清理任务。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishVersion(KnowledgeDocument original, String newVersion, int chunkCount) {
        if (original == null || original.getId() == null || original.getRevision() == null) {
            throw new BusinessException("知识文档管理信息不完整");
        }
        if (newVersion == null || newVersion.isBlank()) {
            throw new BusinessException("知识版本号不能为空");
        }
        if (chunkCount <= 0) {
            throw new BusinessException("知识片段数量必须大于零");
        }

        String normalizedVersion = newVersion.strip();
        if (normalizedVersion.equals(original.getActiveVersion())) {
            throw new BusinessException("新版本号不能与当前生效版本相同");
        }

        KnowledgeDocument update = new KnowledgeDocument();
        update.setId(original.getId());
        update.setActiveVersion(normalizedVersion);
        update.setActiveChunkCount(chunkCount);

        int affectedRows = knowledgeDocumentMapper.switchActiveVersion(update, original.getRevision());
        if (affectedRows != 1) {
            throw new BusinessException("知识文档已被其他请求更新，本次版本未发布，请重新导入");
        }
        // 首次发布没有旧版本，不需要创建清理任务。
        String oldVersion = original.getActiveVersion();
        if (oldVersion != null && !oldVersion.isBlank()) {
            KnowledgeVersionCleanup cleanup = new KnowledgeVersionCleanup();
            cleanup.setDocumentId(original.getId());
            cleanup.setVersion(oldVersion);
            cleanup.setEligibleAt(LocalDateTime.now().plusHours(OLD_VERSION_RETENTION_HOURS));

            int insertedRows = knowledgeVersionCleanupMapper.insert(cleanup);
            if (insertedRows != 1) {
                throw new BusinessException("旧版本清理任务登记失败");
            }
        }
    }

    /**
     * 查询知识文档管理信息。
     */
    @Override
    public KnowledgeDocument findBySourceAndCategory(String source, String category) {
        if (source == null || source.isBlank()) {
            throw new BusinessException("知识来源不能为空");
        }
        if (category == null || category.isBlank()) {
            throw new BusinessException("知识分类不能为空");
        }
        return knowledgeDocumentMapper.selectBySourceAndCategory(source.strip(), category.strip());
    }
}