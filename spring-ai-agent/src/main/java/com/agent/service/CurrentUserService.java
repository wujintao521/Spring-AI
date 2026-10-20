package com.agent.service;

import com.agent.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.stereotype.Service;

/**
 * 从 Spring Security 的认证结果中获取当前用户身份。
 */
@Service
public class CurrentUserService {

    public Long requireUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof BearerTokenAuthentication bearer)
                || !bearer.isAuthenticated()) {
            throw new BusinessException(40101, "请先提供有效的访问凭证", HttpStatus.UNAUTHORIZED);
        }

        // 用户名由 SecurityConfig 创建，目前只可能是1001或1002。
        return Long.valueOf(bearer.getName());
    }
}