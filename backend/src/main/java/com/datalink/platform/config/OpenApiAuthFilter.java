package com.datalink.platform.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 开放 API 鉴权过滤器：仅处理 /api/open/** 路径，校验请求头 X-API-Key 与配置 Token 是否一致。
 * 匹配则写入 ROLE_OPENAPI 认证并继续；不匹配则直接返回 401 JSON，不再向后传递。
 */
@Component
public class OpenApiAuthFilter extends OncePerRequestFilter {

    @Value("${datalink.openapi.token}")
    private String openApiToken;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 非开放 API 路径直接放行，由登录 JWT 体系处理
        if (!request.getRequestURI().startsWith("/api/open")) {
            filterChain.doFilter(request, response);
            return;
        }
        String apiKey = request.getHeader("X-API-Key");
        if (openApiToken != null && openApiToken.equals(apiKey)) {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("openapi", null,
                            List.of(new SimpleGrantedAuthority("ROLE_OPENAPI"))));
            filterChain.doFilter(request, response);
            return;
        }
        // Token 缺失或不匹配：写 401 JSON 并终止链路
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"无效的 API Token\",\"data\":null}");
    }
}
