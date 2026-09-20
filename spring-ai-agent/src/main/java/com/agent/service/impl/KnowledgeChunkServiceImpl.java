package com.agent.service.impl;

import com.agent.exception.BusinessException;
import com.agent.service.KnowledgeChunkService;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 使用 Spring AI 的文本分块器拆分知识内容。
 */
@Service
public class KnowledgeChunkServiceImpl implements KnowledgeChunkService {

    /**
     * 重叠文本的字符数上限，避免复制过长内容；这里不是 Token 数。
     */
    private static final int MAX_OVERLAP_CHARS = 100;

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

        // 先完成基础分块，再为相邻片段补充少量上下文。
        List<Document> chunks = splitter.apply(List.of(new Document(normalizedText)));
        return addSentenceOverlap(chunks);
    }

    /**
     * 为每个后续片段补充前一个原始片段末尾的短句。
     * 只读取原始分块结果，避免已经补充的内容不断向后累积。
     */
    private List<Document> addSentenceOverlap(List<Document> chunks) {
        if (chunks.size() <= 1) {
            return chunks;
        }

        List<Document> results = new ArrayList<>(chunks.size());

        for (int i = 0; i < chunks.size(); i++) {
            Document current = chunks.get(i);
            String content = current.getText();

            if (i > 0) {
                String previousText = chunks.get(i - 1).getText();
                String overlapText = extractOverlapText(previousText);

                if (!overlapText.isBlank()) {
                    content = overlapText + "\n" + content;
                }
            }

            // 保留原片段的编号和元数据，只调整正文。
            results.add(new Document(current.getId(), content, current.getMetadata()));
        }

        return results;
    }

    /**
     * 按中文句末标点和换行寻找末尾短句。
     * 这是针对当前中文规则的简单策略，不是通用的语义识别算法。
     */
    private String extractOverlapText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        // 在句末标点之后或换行处切开，保留句末标点。
        String[] sentences = text.strip().split("(?<=[。！？；])|\\n");
        if (sentences.length == 0) {
            return "";
        }

        String lastSentence = sentences[sentences.length - 1].strip();
        if (lastSentence.isEmpty() || lastSentence.length() > MAX_OVERLAP_CHARS) {
            return "";
        }

        // 如果末尾没有中文句末标点，本次不复制，避免直接复制明显截断的尾部。
        char lastChar = lastSentence.charAt(lastSentence.length() - 1);
        if ("。！？；".indexOf(lastChar) < 0) {
            return "";
        }

        return lastSentence;
    }
}