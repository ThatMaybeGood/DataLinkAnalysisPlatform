package com.workflow.platform.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * 模式条件判断器
 * 根据注解的value值和当前应用模式判断是否创建Bean
 */
public class ModeCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        // 获取当前应用模式
        Environment env = context.getEnvironment();
        String currentMode = env.getProperty("app.mode", "online");

        // 获取注解的值
        String requiredMode = (String) metadata.getAnnotationAttributes("com.workflow.platform.annotation.RequireMode")
                .get("value");

        // 比较当前模式和所需模式
        return requiredMode.equalsIgnoreCase(currentMode);
    }
}