package com.agent.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.web.SecurityFilterChain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   OpaqueTokenIntrospector introspector) throws Exception {
        return http
                // 当前接口通过请求头携带 Bearer Token，不使用 Cookie 登录。
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // 健康检查不要求登录。
                        .requestMatchers("/actuator/health").permitAll()

                        // 这三个知识查询接口，只要求通过身份认证。
                        .requestMatchers(HttpMethod.GET,
                                "/api/order-knowledge/search",
                                "/api/order-knowledge/ask",
                                "/api/order-knowledge/ask-advisor").authenticated()

                        // 知识库其余接口和清理任务接口，要求管理员角色。
                        .requestMatchers(
                                "/api/order-knowledge",
                                "/api/order-knowledge/**",
                                "/api/knowledge-cleanup",
                                "/api/knowledge-cleanup/**").hasRole("ADMIN")

                        // 其他接口继续要求登录，业务归属由原有代码检查。
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .opaqueToken(opaqueToken -> opaqueToken.introspector(introspector)))
                .build();
    }

    @Bean
    public OpaqueTokenIntrospector opaqueTokenIntrospector(
            @Value("${app.auth.user-1001-token}") String token1001,
            @Value("${app.auth.user-1002-token}") String token1002,
            @Value("${app.auth.admin-9001-token}") String adminToken) {

        // 三个测试凭证都必须配置，且长度至少为 32 个字符。
        if (token1001.isBlank() || token1002.isBlank() || adminToken.isBlank()
                || token1001.length() < 32 || token1002.length() < 32
                || adminToken.length() < 32) {
            throw new IllegalArgumentException("测试访问凭证必须配置，且长度至少为32个字符");
        }

        // 防止不同身份使用相同凭证，造成身份混淆。
        if (token1001.equals(token1002)
                || token1001.equals(adminToken)
                || token1002.equals(adminToken)) {
            throw new IllegalArgumentException("不同身份必须使用不同的访问凭证");
        }

        byte[] expected1001 = token1001.getBytes(StandardCharsets.UTF_8);
        byte[] expected1002 = token1002.getBytes(StandardCharsets.UTF_8);
        byte[] expectedAdmin = adminToken.getBytes(StandardCharsets.UTF_8);

        return token -> {
            byte[] received = token.getBytes(StandardCharsets.UTF_8);

            if (MessageDigest.isEqual(expected1001, received)) {
                return createPrincipal("1001", "ROLE_USER");
            }
            if (MessageDigest.isEqual(expected1002, received)) {
                return createPrincipal("1002", "ROLE_USER");
            }
            if (MessageDigest.isEqual(expectedAdmin, received)) {
                return createPrincipal("9001", "ROLE_ADMIN");
            }

            throw new BadOpaqueTokenException("访问凭证无效");
        };
    }

    private OAuth2AuthenticatedPrincipal createPrincipal(String userId, String authority) {
        // 用户编号和角色都由服务端根据凭证确定。
        return new DefaultOAuth2AuthenticatedPrincipal(
                userId,
                Map.of("sub", userId),
                List.of(new SimpleGrantedAuthority(authority)));
    }
}