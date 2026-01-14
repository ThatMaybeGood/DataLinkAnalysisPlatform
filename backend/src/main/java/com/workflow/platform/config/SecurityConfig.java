package com.workflow.platform.config;

import com.workflow.platform.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private AuthService authService;

    @Value("${workflow.platform.security.jwt.secret-key}")
    private String jwtSecret;

    @Value("${workflow.platform.security.jwt.expiration}")
    private long jwtExpiration;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
//
//    @Bean
//    public JwtAuthenticationFilter jwtAuthenticationFilter() {
//        return new JwtAuthenticationFilter(authService, jwtSecret);
//    }
//
//    @Bean
//    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//        http
//                .csrf(csrf -> csrf.disable())
//                .sessionManagement(session -> session
//                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//                .authorizeHttpRequests(auth -> auth
//                        // 公开访问的接口
//                        .requestMatchers(
//                                "/api/auth/**",
//                                "/api/public/**",
//                                "/swagger-ui/**",
//                                "/api-docs/**",
//                                "/h2-console/**",
//                                "/error"
//                        ).permitAll()
//                        // 需要认证的接口
//                        .requestMatchers("/api/**").authenticated()
//                        // 管理员接口
//                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
//                        .anyRequest().authenticated())
//                .addFilterBefore(jwtAuthenticationFilter(),
//                        UsernamePasswordAuthenticationFilter.class)
//                .headers(headers -> headers
//                        .frameOptions(frame -> frame.sameOrigin())
//                        .contentSecurityPolicy(csp -> csp.policyDirectives(
//                                "default-src 'self'; " +
//                                        "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
//                                        "style-src 'self' 'unsafe-inline'; " +
//                                        "img-src 'self' data: https:;" +
//                                        "font-src 'self' data:;"))
//                );
//
//        return http.build();
//    }
}