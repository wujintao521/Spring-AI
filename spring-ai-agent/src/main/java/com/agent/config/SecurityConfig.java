package com.agent.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

/**
 * 使用预配置的测试访问凭证验证用户身份。
 * 当前实现不包含 Token 签发、过期和撤销管理。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   OpaqueTokenIntrospector introspector) throws Exception {
        return http
                // 当前接口仅通过 Authorization 请求头携带 Bearer 凭证，
                // 不使用 Cookie 登录、表单登录或 HTTP Basic 自动认证。
                .csrf(csrf -> csrf.disable())
                // 每次请求独立验证凭证，不通过 Session 保存登录状态。
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .opaqueToken(opaqueToken -> opaqueToken.introspector(introspector)))
                .build();
    }

    @Bean
    public OpaqueTokenIntrospector opaqueTokenIntrospector(
            @Value("${app.auth.user-1001-token}") String token1001,
            @Value("${app.auth.user-1002-token}") String token1002) {

        // 配置错误时直接启动失败，不能退化成不需要认证。
        if (token1001.isBlank() || token1002.isBlank()
                || token1001.length() < 32 || token1002.length() < 32) {
            throw new IllegalArgumentException("测试访问凭证必须配置，且长度至少为32个字符");
        }
        if (token1001.equals(token1002)) {
            throw new IllegalArgumentException("两个用户必须使用不同的访问凭证");
        }

        byte[] expected1001 = token1001.getBytes(StandardCharsets.UTF_8);
        byte[] expected1002 = token1002.getBytes(StandardCharsets.UTF_8);

        // Spring Security 提取请求中的 Bearer Token 后，调用这里进行验证。
        return token -> {
            byte[] received = token.getBytes(StandardCharsets.UTF_8);

            if (MessageDigest.isEqual(expected1001, received)) {
                return createPrincipal("1001");
            }
            if (MessageDigest.isEqual(expected1002, received)) {
                return createPrincipal("1002");
            }

            throw new BadOpaqueTokenException("访问凭证无效");
        };
    }

    private OAuth2AuthenticatedPrincipal createPrincipal(String userId) {
        // 用户身份由服务端根据验证结果创建，不从请求 Body 或模型结果中读取。
        return new DefaultOAuth2AuthenticatedPrincipal(
                userId,
                Map.of("sub", userId),
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}