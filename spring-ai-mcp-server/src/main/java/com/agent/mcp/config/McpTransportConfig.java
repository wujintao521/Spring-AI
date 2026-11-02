package com.agent.mcp.config;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStatelessServerTransport;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.security.Principal;
import java.util.Map;

@Configuration
public class McpTransportConfig {

    @Bean
    public WebMvcStatelessServerTransport webMvcStatelessServerTransport(
            @Qualifier("mcpServerJsonMapper") JsonMapper jsonMapper,
            @Value("${spring.ai.mcp.server.stateless.mcp-endpoint:/mcp}")
            String endpoint) {

        return WebMvcStatelessServerTransport.builder()
                .jsonMapper(new JacksonMcpJsonMapper(jsonMapper))
                .messageEndpoint(endpoint)
                .contextExtractor(request -> {
                    // 请求到这里之前，必须先通过现有Spring Security认证。
                    Principal caller = request.servletRequest().getUserPrincipal();
                    if (caller == null || !"order-agent-service".equals(caller.getName())) {
                        throw new ResponseStatusException(
                                HttpStatus.FORBIDDEN, "不允许传递用户身份");
                    }

                    String userIdHeader = request.headers().firstHeader("X-MCP-User-Id");

                    // 启动时的初始化、工具发现等请求可以没有用户身份。
                    if (userIdHeader == null) {
                        return McpTransportContext.EMPTY;
                    }

                    if (!userIdHeader.matches("[1-9][0-9]{0,18}")) {
                        throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, "用户身份格式不正确");
                    }

                    long userId;
                    try {
                        userId = Long.parseLong(userIdHeader);
                    } catch (NumberFormatException exception) {
                        throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, "用户身份格式不正确");
                    }

                    // 保存为当前MCP请求的上下文，供后续工具读取。
                    return McpTransportContext.create(Map.of("userId", userId));
                })
                .build();
    }
}