package com.datalink.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * DataLink 平台 · 后端启动类
 */
@SpringBootApplication
@MapperScan("com.datalink.platform.**.mapper")
public class DataLinkApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataLinkApplication.class, args);
    }
}
