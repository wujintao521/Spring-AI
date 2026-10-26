package com.agent.advisor;

import com.agent.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 限制单次聊天请求中的主模型调用轮数。
 */
@Component
@Slf4j
public class ModelCallLimitAdvisor implements CallAdvisor {

    public static final String COUNTER_KEY = "agent.modelCallCounter";

    private static final int MAX_MODEL_CALLS = 6;

    @Override
    public String getName() {
        return "ModelCallLimitAdvisor";
    }

    @Override
    public int getOrder() {
        // 放在工具循环内部，每轮请求模型之前检查一次。
        return ToolCallingAdvisor.DEFAULT_ORDER + 1;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        Object value = request.context().get(COUNTER_KEY);

        if (!(value instanceof AtomicInteger counter)) {
            throw new IllegalStateException("缺少本次请求的模型调用计数器");
        }

        // 本次准备进行第几轮模型调用。
        int callNumber = counter.incrementAndGet();

        if (callNumber > MAX_MODEL_CALLS) {
            log.warn("模型调用轮数达到上限 maxCalls={}", MAX_MODEL_CALLS);
            throw new BusinessException("本次处理步骤过多，请简化问题后重试");
        }

        log.info("本次模型调用轮次 round={} maxRounds={}", callNumber, MAX_MODEL_CALLS);

        // 没有超过上限，才继续执行后面的Advisor和模型调用。
        return chain.nextCall(request);
    }
}