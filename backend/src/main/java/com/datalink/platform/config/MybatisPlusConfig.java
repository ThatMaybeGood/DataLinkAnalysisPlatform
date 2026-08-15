package com.datalink.platform.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置：按 datalink.db-type 选择分页方言，并限制单页最大条数。
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 分页插件：mysql 用 MySQL 方言，其余（默认 h2）用 H2 方言。
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(@Value("${datalink.db-type:h2}") String dbType) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor p = "mysql".equalsIgnoreCase(dbType)
                ? new PaginationInnerInterceptor(DbType.MYSQL)
                : new PaginationInnerInterceptor(DbType.H2);
        p.setMaxLimit(500L);
        interceptor.addInnerInterceptor(p);
        return interceptor;
    }
}
