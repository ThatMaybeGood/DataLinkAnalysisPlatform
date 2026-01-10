package com.workflow.platform.aspect;

import com.workflow.platform.util.ModeContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

/**
 * 模式切面
 * 自动处理模式相关的横切关注点，包括：
 * 1. 自动设置请求的模式上下文
 * 2. 验证方法调用的模式权限
 * 3. 记录模式相关的操作日志
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
     * 拦截所有Controller类的方法
     */
    @Pointcut("@within(org.springframework.web.bind.annotation.RestController) || " +
            "@within(org.springframework.stereotype.Controller)")
    public void controllerPointcut() {}

    /**
     * 服务层切入点
     * 拦截所有Service类的方法
     */
    @Pointcut("@within(org.springframework.stereotype.Service)")
    public void servicePointcut() {}

    /**
     * 仓库层切入点
     * 拦截所有Repository类的方法
     */
    @Pointcut("@within(org.springframework.stereotype.Repository)")
    public void repositoryPointcut() {}

    /**
     * 模式验证切入点
     * 拦截带有@RequireMode注解的方法
     */
    @Pointcut("@annotation(com.workflow.platform.annotation.RequireMode)")
    public void requireModePointcut() {}

    /**
     * 控制器和服务层的环绕通知
     * 自动设置模式上下文
     */
    @Around("controllerPointcut() || servicePointcut() || repositoryPointcut()")
    public Object aroundMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        // 保存原始模式上下文
        ModeContext.ContextInfo originalContext = modeContext.getContextInfo();

        try {
            // 设置请求相关的模式上下文
            setupRequestContext();

            // 记录方法调用日志
            logMethodCall(joinPoint);

            // 执行目标方法
            Object result = joinPoint.proceed();

            // 记录方法完成日志
            logMethodCompletion(joinPoint, result);

            return result;

        } catch (Exception e) {
            // 记录方法异常日志
            logMethodException(joinPoint, e);
            throw e;

        } finally {
            // 恢复原始模式上下文
            restoreOriginalContext(originalContext);
        }
    }

    /**
     * 模式验证的环绕通知
     * 检查方法调用是否符合模式要求
     */
    @Around("requireModePointcut()")
    public Object checkMode(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取注解信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        com.workflow.platform.annotation.RequireMode annotation =
                method.getAnnotation(com.workflow.platform.annotation.RequireMode.class);

        if (annotation != null) {
            // 获取要求的模式
            String requiredMode = annotation.value();

            // 获取当前模式（优先从上下文获取，然后从配置文件获取）
            String currentMode = modeContext.getMode();
            if (currentMode == null) {
                currentMode = environment.getProperty("app.mode", "online");
            }

            // 验证模式
            if (!requiredMode.equalsIgnoreCase(currentMode)) {
                String errorMsg = String.format(
                        "方法 '%s.%s' 要求模式为 '%s'，但当前模式为 '%s'",
                        method.getDeclaringClass().getSimpleName(),
                        method.getName(),
                        requiredMode,
                        currentMode
                );
                log.warn(errorMsg);
                throw new ModeNotAllowedException(errorMsg);
            }

            log.debug("模式验证通过: 方法 {} 需要模式 {}，当前模式 {}",
                    method.getName(), requiredMode, currentMode);
        }

        return joinPoint.proceed();
    }

    // ========== 私有辅助方法 ==========

    /**
     * 设置请求相关的模式上下文
     */
    private void setupRequestContext() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();

            // 从请求头获取模式信息
            String requestMode = request.getHeader("X-App-Mode");
            if (requestMode != null && ("online".equals(requestMode) || "offline".equals(requestMode))) {
                modeContext.setMode(requestMode);
            } else {
                // 使用配置文件中的模式
                String configMode = environment.getProperty("app.mode", "online");
                modeContext.setMode(configMode);
            }

            // 从请求头获取租户信息
            String tenantId = request.getHeader("X-Tenant-Id");
            if (tenantId != null) {
                modeContext.setTenantId(tenantId);
            }

            // 从请求头获取用户信息
            String userId = request.getHeader("X-User-Id");
            if (userId != null) {
                modeContext.setUserId(userId);
            }

            // 记录请求上下文
            if (log.isDebugEnabled()) {
                ModeContext.ContextInfo context = modeContext.getContextInfo();
                log.debug("设置请求上下文: mode={}, tenant={}, user={}",
                        context.getMode(), context.getTenantId(), context.getUserId());
            }
        }
    }

    /**
     * 恢复原始模式上下文
     */
    private void restoreOriginalContext(ModeContext.ContextInfo originalContext) {
        modeContext.clear();

        if (originalContext != null) {
            if (originalContext.getMode() != null) {
                modeContext.setMode(originalContext.getMode());
            }
            if (originalContext.getTenantId() != null) {
                modeContext.setTenantId(originalContext.getTenantId());
            }
            if (originalContext.getUserId() != null) {
                modeContext.setUserId(originalContext.getUserId());
            }
            if (originalContext.getAttributes() != null) {
                originalContext.getAttributes().forEach(modeContext::setAttribute);
            }
        }
    }

    /**
     * 记录方法调用日志
     */
    private void logMethodCall(ProceedingJoinPoint joinPoint) {
        if (log.isDebugEnabled()) {
            String className = joinPoint.getTarget().getClass().getSimpleName();
            String methodName = joinPoint.getSignature().getName();
            String mode = modeContext.getMode();

            log.debug("执行方法: {}.{}，当前模式: {}", className, methodName, mode);
        }
    }

    /**
     * 记录方法完成日志
     */
    private void logMethodCompletion(ProceedingJoinPoint joinPoint, Object result) {
        if (log.isDebugEnabled()) {
            String className = joinPoint.getTarget().getClass().getSimpleName();
            String methodName = joinPoint.getSignature().getName();

            log.debug("方法完成: {}.{}，返回类型: {}",
                    className, methodName,
                    result != null ? result.getClass().getSimpleName() : "void");
        }
    }

    /**
     * 记录方法异常日志
     */
    private void logMethodException(ProceedingJoinPoint joinPoint, Exception e) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String mode = modeContext.getMode();

        log.error("方法执行异常: {}.{}，模式: {}，异常: {}",
                className, methodName, mode, e.getMessage(), e);
    }

    /**
     * 模式不允许异常
     */
    public static class ModeNotAllowedException extends RuntimeException {
        public ModeNotAllowedException(String message) {
            super(message);
        }
    }
}