package com.datalink.platform.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 安全配置：无状态 + RBAC 授权。
 * 公开：登录/健康检查/接口文档；建模写操作限 ADMIN/MODELER；告警工单写操作限 ADMIN/OPERATOR/ONCALL；其余需登录。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 密码编码器（BCrypt，强度 10，与种子数据哈希一致）
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    /**
     * 安全过滤链：无状态 + RBAC 授权 + JWT 前置过滤器
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter,
                                                   OpenApiAuthFilter openApiAuthFilter) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 公开接口：登录 / 健康检查 / 监控端点 / 接口文档 / 错误页
                        .requestMatchers("/api/auth/login", "/api/health", "/actuator/**",
                                "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/error",
                                "/api/open/**").permitAll()
                        // CORS 预检
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 建模/数据池写操作：仅 ADMIN / MODELER
                        .requestMatchers(HttpMethod.POST, "/api/nodes", "/api/processes", "/api/routes", "/api/relations", "/api/edges", "/api/connectors").hasAnyRole("ADMIN", "MODELER")
                        .requestMatchers(HttpMethod.PUT, "/api/nodes/**", "/api/processes/**", "/api/routes/**", "/api/connectors/**").hasAnyRole("ADMIN", "MODELER")
                        .requestMatchers(HttpMethod.DELETE, "/api/nodes/**", "/api/processes/**", "/api/routes/**", "/api/relations/**", "/api/edges/**", "/api/connectors/**").hasAnyRole("ADMIN", "MODELER")
                        // 告警/工单写操作：ADMIN / OPERATOR / ONCALL
                        .requestMatchers(HttpMethod.POST, "/api/alerts/**", "/api/tickets").hasAnyRole("ADMIN", "OPERATOR", "ONCALL")
                        .requestMatchers(HttpMethod.PUT, "/api/tickets/**", "/api/alerts/**").hasAnyRole("ADMIN", "OPERATOR", "ONCALL")
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        // 未登录/登录过期：HTTP 401 + 统一 JSON
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"code\":401,\"message\":\"未认证或登录已过期\",\"data\":null}");
                        })
                        // 已登录但角色不足：HTTP 403 + 统一 JSON
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"code\":403,\"message\":\"无权限\",\"data\":null}");
                        }))
                .addFilterBefore(openApiAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
