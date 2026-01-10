package com.workflow.platform.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${spring.cache.caffeine.spec:maximumSize=1000,expireAfterWrite=3600s}")
    private String caffeineSpec;

    @Bean
    @Primary
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // 配置Caffeine
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(3600, TimeUnit.SECONDS)
                .recordStats();

        cacheManager.setCaffeine(caffeine);
        cacheManager.setCacheNames(java.util.Arrays.asList(
                "workflows", "nodes", "rules", "executions", "users"
        ));

        return cacheManager;
    }
}