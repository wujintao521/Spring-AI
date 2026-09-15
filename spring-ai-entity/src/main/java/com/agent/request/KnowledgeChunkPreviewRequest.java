package com.agent.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 知识文本分块预览请求。
 *
 * @param text 待拆分的文本
 * @param chunkSize 每个片段的目标 Token 数量
 */
public record KnowledgeChunkPreviewRequest(
        @NotBlank(message = "待分块的文本不能为空")
        @Size(max = 20000, message = "预览文本不能超过20000个字符")
        String text,

        @NotNull(message = "分块大小不能为空")
        @Min(value = 100, message = "分块大小不能小于100")
        @Max(value = 2000, message = "分块大小不能大于2000")
        Integer chunkSize) {
}