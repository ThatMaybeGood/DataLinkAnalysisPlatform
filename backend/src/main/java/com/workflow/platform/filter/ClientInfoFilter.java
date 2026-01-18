package com.workflow.platform.filter;

import lombok.extern.slf4j.Slf4j;
// import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.UUID;

/**
 * 客户端信息过滤器
 */
@Slf4j
public class ClientInfoFilter implements Filter {

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {

		HttpServletRequest httpRequest = (HttpServletRequest) request;

		// 提取或生成客户端信息
		String clientId = extractOrGenerateClientId(httpRequest);
		String sessionId = httpRequest.getSession().getId();

		// 设置请求属性
		httpRequest.setAttribute("clientId", clientId);
		httpRequest.setAttribute("sessionId", sessionId);

		// 记录客户端信息
		logClientInfo(httpRequest, clientId, sessionId);

		chain.doFilter(request, response);
	}

	private String extractOrGenerateClientId(HttpServletRequest request) {
		// 尝试从请求中提取客户端ID
		String clientId = request.getHeader("X-Client-Id");

		if (clientId == null || clientId.trim().isEmpty()) {
			clientId = request.getParameter("clientId");
		}

		// 如果都没有，生成一个
		if (clientId == null || clientId.trim().isEmpty()) {
			clientId = "client_" + UUID.randomUUID().toString().substring(0, 8);
			log.debug("生成新的客户端ID: {}", clientId);
		}

		return clientId;
	}

	private void logClientInfo(HttpServletRequest request, String clientId, String sessionId) {
		String requestURI = request.getRequestURI();
		String userAgent = request.getHeader("User-Agent");
		String remoteAddr = request.getRemoteAddr();

		log.debug("客户端请求 - ID: {}, Session: {}, URI: {}, IP: {}, Agent: {}",
				clientId, sessionId, requestURI, remoteAddr, userAgent);
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		log.info("初始化客户端信息过滤器");
	}

	@Override
	public void destroy() {
		log.info("销毁客户端信息过滤器");
	}
}