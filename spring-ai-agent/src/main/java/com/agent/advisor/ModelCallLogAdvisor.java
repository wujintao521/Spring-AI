package com.agent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 记录每一轮非流式聊天模型调用的耗时和Token用量。
 */
@Component
@Slf4j
public class ModelCallLogAdvisor implements CallAdvisor {

    @Override
    public String getName() {
        return "ModelCallLogAdvisor";
    }

    @Override
    public int getOrder() {
        /*
         * 放在默认工具调用Advisor之后，进入它内部的每轮模型调用。
         * 如果放在工具循环外面，就只能观察整个循环一次。
         */
        return ToolCallingAdvisor.DEFAULT_ORDER + 1;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        // 一次HTTP请求可能调用多轮模型，每轮再生成独立的调用标识。
        String modelCallId = UUID.randomUUID().toString().replace("-", "");
        long startNanos = System.nanoTime();

        log.info("模型调用开始 modelCallId={}", modelCallId);

        try {
            // 继续后面的处理链，直到模型返回本轮结果。
            ChatClientResponse response = chain.nextCall(request);
            ChatResponse chatResponse = response.chatResponse();

            String model = null;
            Usage usage = null;
            if (chatResponse != null) {
                model = chatResponse.getMetadata().getModel();
                usage = chatResponse.getMetadata().getUsage();
            }

            // 未取得用量对象时保留null，不自行补成0。
            Integer inputTokens = usage == null ? null : usage.getPromptTokens();
            Integer outputTokens = usage == null ? null : usage.getCompletionTokens();
            Integer totalTokens = usage == null ? null : usage.getTotalTokens();

            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            log.info("模型调用结束 modelCallId={} model={} inputTokens={} outputTokens={} totalTokens={} elapsedMs={}",
                    modelCallId, model, inputTokens, outputTokens, totalTokens, elapsedMs);

            // 原样返回，不修改模型结果。
            return response;
        } catch (RuntimeException exception) {
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            log.error("模型调用失败 modelCallId={} elapsedMs={}", modelCallId, elapsedMs, exception);
            throw exception;
        }
    }
}