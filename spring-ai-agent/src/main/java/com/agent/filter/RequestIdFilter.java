package com.agent.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 为HTTP请求生成日志关联标识，并记录处理耗时。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class RequestIdFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_KEY = "requestId";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 每次请求生成一个独立标识，与conversationId无关。
        String requestId = UUID.randomUUID().toString().replace("-", "");

        // 使用单调时钟测量经过的时间。
        long startNanos = System.nanoTime();

        // 将标识放入当前线程的日志上下文。
        MDC.put(REQUEST_ID_KEY, requestId);

        try {
            // 前端或Postman可以通过响应头取得本次请求标识。
            response.setHeader(REQUEST_ID_HEADER, requestId);

            log.info("请求开始 method={} path={}", request.getMethod(), request.getRequestURI());

            // 继续执行后面的Filter、Controller和业务代码。
            filterChain.doFilter(request, response);
        } finally {
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            log.info("请求结束 method={} path={} elapsedMs={}", request.getMethod(), request.getRequestURI(), elapsedMs);
            // Web线程会复用，处理结束必须清理，避免影响后面的请求。
            MDC.remove(REQUEST_ID_KEY);
        }
    }
}