package com.workflow.platform.annotation;

import com.workflow.platform.config.ModeCondition;
import org.springframework.context.annotation.Conditional;
import java.lang.annotation.*;

/**
 * 模式条件注解
 * 用于标记方法或类只在特定模式下生效
 *
 * 使用示例:
 * @RequireMode("online")  - 只在在线模式下生效
 * @RequireMode("offline") - 只在离线模式下生效
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(ModeCondition.class)
public @interface RequireMode {
    /**
     * 需要的模式
     * @return "online" 或 "offline"
     */
    String value();
}