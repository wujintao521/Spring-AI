package com.agent.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 知识文档管理信息。
 * 管理文档当前生效的版本，正文和向量仍保存在向量库中。
 */
@Getter
@Setter
@NoArgsConstructor
public class KnowledgeDocument {

    /**
     * 文档主键。
     */
    private Long id;

    /**
     * 知识来源标识，例如文件的资源路径。
     */
    private String source;

    /**
     * 知识所属的业务分类。
     */
    private String category;

    /**
     * 当前供检索使用的版本号，为空表示尚未发布。
     */
    private String activeVersion;

    /**
     * 当前生效版本包含的知识片段数量。
     */
    private Integer activeChunkCount;

    /**
     * 修订次数，用于防止并发发布时相互覆盖。
     */
    private Long revision;

    /**
     * 文档管理记录的创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 文档管理记录的最后更新时间。
     */
    private LocalDateTime updatedAt;
}