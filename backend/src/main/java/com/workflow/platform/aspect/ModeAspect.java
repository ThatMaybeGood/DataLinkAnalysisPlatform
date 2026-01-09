package com.workflow.platform.aspect;

import com.workflow.platform.util.ModeContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

/**
 * 模式切面 - 用于自动设置模式上下文
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class ModeAspect {

    private final Environment environment;
    private final ModeContext modeContext;

    /**
     * 控制器切入点
     */
    @Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
    public void controllerPointcut() {}

    /**
     * 服务切入点
     */
    @Pointcut("@within(org.springframework.stereotype.Service)")
    public void servicePointcut() {}

    /**
     * 环绕通知 - 自动设置模式上下文
     */
    @Around("controllerPointcut() || servicePointcut()")
    public Object aroundMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        // 保存原始模式上下文
        String originalMode = modeContext.getMode();
        String originalTenant = modeContext.getTenant();
        String originalUserId = modeContext.getUserId();

        try {
            // 获取当前请求（如果有）
            ServletRequestAttributes attributes = (ServletRequestAttributes)
                    RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();

                // 从请求头获取模式信息
                String requestMode = request.getHeader("X-App-Mode");
                String tenantId = request.getHeader("X-Tenant-Id");
                String userId = request.getHeader("X-User-Id");

                // 设置模式上下文
                if (requestMode != null && ("online".equals(requestMode) || "offline".equals(requestMode))) {
                    modeContext.setMode(requestMode);
                } else {
                    // 使用配置文件中的模式
                    modeContext.setMode(environment.getProperty("app.mode", "online"));
                }

                if (tenantId != null) {
                    modeContext.setTenant(tenantId);
                }

                if (userId != null) {
                    modeContext.setUserId(userId);
                }
            } else {
                // 非Web请求，使用配置文件中的模式
                modeContext.setMode(environment.getProperty("app.mode", "online"));
            }

            // 记录模式信息
            log.debug("执行方法: {}, 模式: {}, 租户: {}, 用户: {}",
                    joinPoint.getSignature().toShortString(),
                    modeContext.getMode(),
                    modeContext.getTenant(),
                    modeContext.getUserId());

            // 执行目标方法
            return joinPoint.proceed();

        } finally {
            // 恢复原始模式上下文
            modeContext.setMode(originalMode);
            modeContext.setTenant(originalTenant);
            modeContext.setUserId(originalUserId);
        }
    }

    /**
     * 模式验证切面
     */
    @Around("@annotation(com.workflow.platform.annotation.RequireMode)")
    public Object checkMode(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequireMode annotation = method.getAnnotation(RequireMode.class);

        // 检查模式
        String requiredMode = annotation.value();
        String currentMode = modeContext.getMode();

        if (!requiredMode.equals(currentMode)) {
            throw new ModeNotAllowedException(
                    String.format("当前模式为 %s，但方法要求模式为 %s", currentMode, requiredMode));
        }

        return joinPoint.proceed();
    }
}

/**
 * 模式注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireMode {
    String value(); // online 或 offline
}

/**
 * 模式不允许异常
 */
class ModeNotAllowedException extends RuntimeException {
    public ModeNotAllowedException(String message) {
        super(message);
    }
}