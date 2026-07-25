package com.example.personalmanager.security;

import com.example.personalmanager.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * REST 风格的认证与授权异常处理器，统一处理未认证（401）与无权限（403）场景：
 * 页面请求重定向至首页，接口请求返回 JSON 错误响应。
 */
@Component
public class RestAuthHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 处理未认证请求（认证入口点）。
     *
     * @param request       HTTP 请求
     * @param response      HTTP 响应
     * @param authException 认证异常
     */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        // 页面请求重定向至首页，由前端处理登录跳转
        if (isPageRequest(request)) {
            response.sendRedirect("/");
            return;
        }

        // 接口请求返回 401 及 JSON 错误信息
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.fail("未认证或Token无效")));
    }

    /**
     * 处理已认证但无访问权限的请求。
     *
     * @param request               HTTP 请求
     * @param response              HTTP 响应
     * @param accessDeniedException 访问拒绝异常
     */
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        // 页面请求重定向至首页
        if (isPageRequest(request)) {
            response.sendRedirect("/");
            return;
        }

        // 接口请求返回 403 及 JSON 错误信息
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.fail("无权限访问")));
    }

    // 判断是否为页面请求（以 .html 结尾或为根路径）
    private boolean isPageRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && (uri.endsWith(".html") || "/".equals(uri));
    }
}
