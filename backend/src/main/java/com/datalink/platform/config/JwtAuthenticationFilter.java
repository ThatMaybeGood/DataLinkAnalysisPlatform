package com.datalink.platform.config;

import com.datalink.platform.system.mapper.SysUserMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JWT 认证过滤器：解析 Authorization: Bearer 令牌，加载用户角色后写入认证上下文。
 * 非阻断：令牌无效时不设认证，由授权规则拦下并返回 401。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final SysUserMapper sysUserMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtUtil.validate(token)) {
                String username = jwtUtil.parseUsername(token);
                // 加载角色：用户不存在或角色未绑定则授权为空列表（仅认证、无任何权限）
                Long userId = sysUserMapper.selectIdByUsername(username);
                List<GrantedAuthority> authorities = new ArrayList<>();
                if (userId != null) {
                    for (String roleCode : sysUserMapper.selectRoleCodes(userId)) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + roleCode));
                    }
                }
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(username, null, authorities));
            }
        }
        filterChain.doFilter(request, response);
    }
}
