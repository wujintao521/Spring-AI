package com.agent.mcp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.web.SecurityFilterChain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

@Configuration
public class McpSecurityConfig {

    @Bean
    public SecurityFilterChain mcpSecurityFilterChain(
            HttpSecurity http,
            OpaqueTokenIntrospector introspector) throws Exception {

        return http
                // 当前使用请求头Bearer凭证，不使用Cookie登录。
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // 访问MCP入口需要通过服务身份认证。
                        .requestMatchers("/mcp").hasRole("MCP_CLIENT")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .opaqueToken(opaqueToken ->
                                opaqueToken.introspector(introspector)))
                .build();
    }

    @Bean
    public OpaqueTokenIntrospector mcpTokenIntrospector(
            @Value("${app.mcp.service-token}") String configuredToken) {

        if (configuredToken.isBlank() || configuredToken.length() < 32) {
            throw new IllegalArgumentException("MCP服务凭证必须配置，且至少32个字符");
        }

        byte[] expected = configuredToken.getBytes(StandardCharsets.UTF_8);

        // 请求到达时，Spring Security将提取出的Token传给这里。
        return token -> {
            byte[] received = token.getBytes(StandardCharsets.UTF_8);

            if (!MessageDigest.isEqual(expected, received)) {
                throw new BadOpaqueTokenException("MCP服务凭证无效");
            }

            // 这里只确认调用应用身份，不代表最终用户是1001或1002。
            return new DefaultOAuth2AuthenticatedPrincipal(
                    "order-agent-service",
                    Map.of("sub", "order-agent-service"),
                    List.of(new SimpleGrantedAuthority("ROLE_MCP_CLIENT")));
        };
    }
}