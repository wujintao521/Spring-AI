package com.agent.service.impl;

import com.agent.exception.BusinessException;
import com.agent.service.KnowledgeChunkService;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 使用 Spring AI 的文本分块器拆分知识内容。
 */
@Service
public class KnowledgeChunkServiceImpl implements KnowledgeChunkService {

    /**
     * 按目标 Token 数量分块，并尽量在标点处结束片段。
     */
    @Override
    public List<Document> splitText(String text, int chunkSize) {
        if (text == null || text.isBlank()) {
            throw new BusinessException("待分块的文本不能为空");
        }
        if (chunkSize < 100 || chunkSize > 2000) {
            throw new BusinessException("分块大小必须在100到2000之间");
        }

        // 统一换行格式，移除全文首尾的空白。
        String normalizedText = text.replace("\r\n", "\n").replace("\r", "\n").strip();

        // 这些参数先用于学习和预览，后续根据检索效果调整。
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(chunkSize)     // 设置某块的token数量
                .withMinChunkSizeChars(50)    // 标点位置超过这个字符位置时，才考虑在该标点处提前切断；不是保证每块至少50字
                .withMinChunkLengthToEmbed(0) // 不因为片段太短而过滤掉非空内容，避免短规则被忽略
                .withKeepSeparator(true)      // 保留片段内部的换行
                .withPunctuationMarks(List.of('。', '！', '？', '；', '.', '!', '?', ';', '\n')) // 将中文标点也纳入切分边界的选择
                .build();

        // Document 只是文本载体；这里没有调用 Embedding 或向量库。
        return splitter.apply(List.of(new Document(normalizedText)));
    }
}