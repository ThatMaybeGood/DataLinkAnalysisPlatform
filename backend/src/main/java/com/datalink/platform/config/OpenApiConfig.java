package com.datalink.platform.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 文档定义：接口标题、版本与描述。
 */
@Configuration
@OpenAPIDefinition(info = @Info(
        title = "DataLink 平台 API",
        version = "0.1.0",
        description = "数据关联与业务流程监控分析平台后端接口",
        contact = @Contact(name = "DataLink Team")))
public class OpenApiConfig {
}
