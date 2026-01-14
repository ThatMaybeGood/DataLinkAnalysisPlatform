package com.workflow.platform.config;

import com.workflow.platform.filter.ClientInfoFilter;
import com.workflow.platform.filter.LogFilter;
import com.workflow.platform.filter.ModeCheckFilter;
import com.workflow.platform.interceptor.ModeConsistencyInterceptor;
import com.workflow.platform.interceptor.ModeInterceptor;
import com.workflow.platform.interceptor.RequestLogInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * Web配置类
 * 配置CORS、静态资源、拦截器等
 */



@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${workflow.platform.security.cors.allowed-origins:*}")
    private String[] allowedOrigins;

    @Value("${workflow.platform.security.cors.allowed-methods:*}")
    private String[] allowedMethods;

    @Value("${workflow.platform.security.cors.allowed-headers:*}")
    private String[] allowedHeaders;

    @Value("${workflow.platform.security.cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${workflow.platform.security.cors.max-age:3600}")
    private long maxAge;



    @Autowired
    private ModeConsistencyInterceptor modeConsistencyInterceptor;

    @Autowired
    private RequestLogInterceptor requestLogInterceptor;




    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 添加模式检查拦截器
        registry.addInterceptor(new ModeInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/public/**", "/api/auth/**");

        // 添加请求日志拦截器
        registry.addInterceptor(new RequestLogInterceptor())
                .addPathPatterns("/api/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 静态资源映射
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");

        // Swagger UI资源
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/springfox-swagger-ui/");
    }

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(allowCredentials);
        config.setAllowedOrigins(Arrays.asList(allowedOrigins));
        config.setAllowedMethods(Arrays.asList(allowedMethods));
        config.setAllowedHeaders(Arrays.asList(allowedHeaders));
        config.setMaxAge(maxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }



    @Bean
    public void addModeInterceptors(InterceptorRegistry registry) {
        // 添加模式一致性拦截器
        registry.addInterceptor(modeConsistencyInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/public/**", "/api/auth/**", "/api/coordination/heartbeat");

        // 添加请求日志拦截器
        registry.addInterceptor(requestLogInterceptor)
                .addPathPatterns("/api/**");
    }

    @Bean
    public FilterRegistrationBean<ClientInfoFilter> clientInfoFilter() {
        FilterRegistrationBean<ClientInfoFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new ClientInfoFilter());
        registrationBean.addUrlPatterns("/api/*");
        registrationBean.setOrder(1);
        registrationBean.setName("clientInfoFilter");
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<ModeCheckFilter> modeCheckFilter() {
        FilterRegistrationBean<ModeCheckFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new ModeCheckFilter());
        registrationBean.addUrlPatterns("/api/*");
        registrationBean.setOrder(2);
        registrationBean.setName("modeCheckFilter");
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<LogFilter> logFilter() {
        FilterRegistrationBean<LogFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new LogFilter());
        registrationBean.addUrlPatterns("/api/*");
        registrationBean.setOrder(3);
        registrationBean.setName("logFilter");
        return registrationBean;
    }

}