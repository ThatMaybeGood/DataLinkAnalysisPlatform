package com.workflow.platform.interceptor;

import com.workflow.platform.component.ModeConsistencyChecker;
import com.workflow.platform.component.ModeManager;
import com.workflow.platform.enums.ModeType;
import com.workflow.platform.exception.ModeConsistencyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;

/**
 * 模式一致性拦截器
 */
@Slf4j
@Component
public class ModeConsistencyInterceptor implements HandlerInterceptor {

    @Autowired
    private ModeManager modeManager;

    @Autowired
    private ModeConsistencyChecker modeConsistencyChecker;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {

        String requestURI = request.getRequestURI();

        // 跳过某些路径
        if (shouldSkip(requestURI)) {
            return true;
        }

        // 获取客户端模式信息
        String clientId = extractClientId(request);
        String clientModeStr = extractClientMode(request);

        if (clientId != null && clientModeStr != null) {
            try {
                ModeType clientMode = ModeType.fromCode(clientModeStr);
                ModeType serverMode = modeManager.getCurrentMode();

                // 检查模式一致性
                if (!isModeConsistent(clientMode, serverMode)) {
                    log.warn("模式不一致: 客户端={}，客户端模式={}，服务器模式={}，请求URI={}",
                            clientId, clientMode, serverMode, requestURI);

                    // 记录不一致
                    modeConsistencyChecker.registerClientMode(clientId, clientMode,
                            request.getSession().getId());

                    // 对于重要操作，抛出异常
                    if (isCriticalOperation(requestURI)) {
                        throw new ModeConsistencyException(
                                "模式不一致，操作被拒绝",
                                clientId, clientMode.getCode(), serverMode.getCode()
                        );
                    }

                    // 添加警告头
                    response.addHeader("X-Mode-Warning",
                            "客户端模式与服务器模式不一致");
                }

            } catch (IllegalArgumentException e) {
                log.warn("无效的客户端模式: {}", clientModeStr);
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) throws Exception {

        // 记录请求完成后的模式状态
        String clientId = extractClientId(request);
        if (clientId != null) {
            // 可以在这里更新客户端最后活动时间等
        }
    }

    private boolean shouldSkip(String requestURI) {
        // 跳过静态资源、公开API等
        return requestURI.startsWith("/static/") ||
                requestURI.startsWith("/api/public/") ||
                requestURI.startsWith("/api/auth/") ||
                requestURI.startsWith("/swagger-ui/") ||
                requestURI.startsWith("/api-docs/");
    }

    private String extractClientId(HttpServletRequest request) {
        // 从多种来源提取客户端ID
        String clientId = request.getHeader("X-Client-Id");

        if (clientId == null || clientId.trim().isEmpty()) {
            clientId = request.getParameter("clientId");
        }

        if (clientId == null || clientId.trim().isEmpty()) {
            // 尝试从Session或Token中提取
            // 这里简化为从Session属性获取
            Object sessionClientId = request.getSession().getAttribute("clientId");
            if (sessionClientId != null) {
                clientId = sessionClientId.toString();
            }
        }

        return clientId;
    }

    private String extractClientMode(HttpServletRequest request) {
        // 从多种来源提取客户端模式
        String clientMode = request.getHeader("X-Client-Mode");

        if (clientMode == null || clientMode.trim().isEmpty()) {
            clientMode = request.getParameter("clientMode");
        }

        if (clientMode == null || clientMode.trim().isEmpty()) {
            // 尝试从Session或Token中提取
            Object sessionClientMode = request.getSession().getAttribute("clientMode");
            if (sessionClientMode != null) {
                clientMode = sessionClientMode.toString();
            }
        }

        return clientMode;
    }

    private boolean isModeConsistent(ModeType clientMode, ModeType serverMode) {
        // 一致性检查逻辑
        if (serverMode == ModeType.MIXED) {
            return true;
        }

        return clientMode == serverMode;
    }

    private boolean isCriticalOperation(String requestURI) {
        // 定义关键操作路径
        return requestURI.contains("/api/workflow/") &&
                (requestURI.contains("/create") ||
                        requestURI.contains("/update") ||
                        requestURI.contains("/delete") ||
                        requestURI.contains("/execute"));
    }
}