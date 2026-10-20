package com.agent.service.impl;

import com.agent.entity.KnowledgeDocument;
import com.agent.service.KnowledgeChunkService;
import com.agent.service.KnowledgeDocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 知识检索服务的版本隔离单元测试。
 *
 * 模拟外部依赖，只检查业务逻辑及实际传出的检索条件。
 */
class OrderKnowledgeServiceImplTest {

    private static final String SOURCE = "knowledge/order-cancellation.txt";
    private static final String CATEGORY = "ORDER_CANCELLATION_FILE";

    private VectorStore vectorStore;
    private KnowledgeDocumentService knowledgeDocumentService;
    private OrderKnowledgeServiceImpl service;

    @BeforeEach
    void setUp() {
        // 创建模拟对象，不连接真实向量库和数据库。
        vectorStore = mock(VectorStore.class);
        knowledgeDocumentService = mock(KnowledgeDocumentService.class);
        KnowledgeChunkService chunkService = mock(KnowledgeChunkService.class);

        // 服务构造方法需要创建聊天客户端，因此提供模拟 Builder。
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        when(builder.clone()).thenReturn(builder);
        when(builder.defaultSystem(anyString())).thenReturn(builder);
        when(builder.build()).thenReturn(mock(ChatClient.class));

        service = new OrderKnowledgeServiceImpl(vectorStore, builder, knowledgeDocumentService, chunkService);
    }

    /**
     * 有管理记录但没有生效版本，说明尚未发布。
     * 即使向量库中可能残留数据，也不能直接检索。
     */
    @Test
    void shouldNotSearchVectorStoreWhenKnowledgeIsUnpublished() {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setSource(SOURCE);
        document.setCategory(CATEGORY);
        document.setActiveVersion(null);

        when(knowledgeDocumentService.findBySourceAndCategory(SOURCE, CATEGORY)).thenReturn(document);

        var results = service.searchKnowledge("已发货订单能取消吗？");

        // 返回空结果，而且不能调用向量库。
        assertTrue(results.isEmpty());
        verifyNoInteractions(vectorStore);
    }

    /**
     * 检查实际发送给向量库的请求是否限定了当前生效版本。
     */
    @Test
    void shouldRestrictFileSearchToActiveVersion() {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setSource(SOURCE);
        document.setCategory(CATEGORY);
        document.setActiveVersion("version-002");

        when(knowledgeDocumentService.findBySourceAndCategory(SOURCE, CATEGORY)).thenReturn(document);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        String question = "已发货订单能取消吗？";
        service.searchKnowledge(question);

        // 捕获服务实际传给向量库的参数。
        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());

        SearchRequest actualRequest = captor.getValue();

        FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();
        Filter.Expression expectedFilter = filterBuilder.and(
                        filterBuilder.eq("category", CATEGORY),
                        filterBuilder.and(
                                filterBuilder.eq("source", SOURCE),
                                filterBuilder.eq("version", "version-002")))
                .build();

        assertEquals(question, actualRequest.getQuery());
        assertEquals(expectedFilter, actualRequest.getFilterExpression());
    }
}