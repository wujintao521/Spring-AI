package com.agent.config;

import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import org.springframework.ai.mcp.customizer.McpClientCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpRequest;

@Configuration
public class McpClientSecurityConfig {

    @Bean
    public McpClientCustomizer<HttpClientStreamableHttpTransport.Builder> mcpServiceAuthentication(@Value("${app.mcp.service-token}") String serviceToken) {
        if (serviceToken.isBlank() || serviceToken.length() < 32) {
            throw new IllegalArgumentException("MCP服务凭证必须配置，且至少32个字符");
        }
        // 创建MCP连接时，框架调用这个定制逻辑。
        return (connectionName, transportBuilder) -> {
            // 只给指定连接添加凭证，避免发给其他MCP Server。
            if ("mdc-server-1".equals(connectionName)) {
                transportBuilder.requestBuilder(
                        HttpRequest.newBuilder()
                                .setHeader("Authorization", "Bearer " + serviceToken));
            }
        };
    }
}