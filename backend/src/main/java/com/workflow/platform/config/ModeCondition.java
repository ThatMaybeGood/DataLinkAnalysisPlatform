// backend/src/main/java/com/workflow/platform/config/ModeCondition.java
package com.workflow.platform.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.env.Environment;

public class ModeCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment env = context.getEnvironment();
        String mode = env.getProperty("app.mode", "online");
        String requiredMode = (String) metadata.getAnnotationAttributes(ConditionalOnMode.class).get("value");
        return requiredMode.equalsIgnoreCase(mode);
    }
}