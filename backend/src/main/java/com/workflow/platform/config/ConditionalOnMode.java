// backend/src/main/java/com/workflow/platform/config/ConditionalOnMode.java
package com.workflow.platform.config;

import org.springframework.context.annotation.Conditional;
import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(ModeCondition.class)
public @interface ConditionalOnMode {
    String value();
}