package com.agent.config;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.common.McpTransportContext;
import org.springframework.ai.mcp.customizer.McpClientCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;

import java.util.Map;

@Configuration
public class McpClientSecurityConfig {

    // 与你当前YAML中的连接名称一致。
    private static final String CONNECTION_NAME = "mdc-server-1";

    /**
     * 每次发起同步MCP调用时，提取当前请求的认证身份。
     */
    @Bean
    public McpClientCustomizer<McpClient.SyncSpec> mcpUserContext() {
        return (connectionName, clientBuilder) -> {
            if (CONNECTION_NAME.equals(connectionName)) {
                clientBuilder.transportContextProvider(() -> {
                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                    // 应用启动时的协议初始化没有登录用户，这是正常情况。
                    if (!(authentication instanceof BearerTokenAuthentication bearer)
                            || !bearer.isAuthenticated()) {
                        return McpTransportContext.EMPTY;
                    }

                    // 用户身份来自认证结果，不读取前端Header或模型参数。
                    return McpTransportContext.create(
                            Map.of("userId", bearer.getName()));
                });
            }
        };
    }

    /**
     * 发送HTTP请求前，把服务凭证和本次调用的用户身份放入Header。
     */
    @Bean
    public McpClientCustomizer<HttpClientStreamableHttpTransport.Builder> mcpServiceAuthentication(@Value("${app.mcp.service-token}") String serviceToken) {

        if (serviceToken.isBlank() || serviceToken.length() < 32) {
            throw new IllegalArgumentException("MCP服务凭证必须配置，且至少32个字符");
        }

        return (connectionName, transportBuilder) -> {
            if (CONNECTION_NAME.equals(connectionName)) {
                transportBuilder.httpRequestCustomizer(
                        (requestBuilder, method, endpoint, body, context) -> {
                            requestBuilder.setHeader(
                                    "Authorization", "Bearer " + serviceToken);

                            // 从本次调用的MCP上下文读取，不在这里读取线程登录信息。
                            Object userId = context.get("userId");
                            if (userId != null) {
                                requestBuilder.setHeader(
                                        "X-MCP-User-Id", userId.toString());
                            }
                        });
            }
        };
    }
}