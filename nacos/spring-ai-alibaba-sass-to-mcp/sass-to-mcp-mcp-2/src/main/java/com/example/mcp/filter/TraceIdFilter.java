package com.example.mcp.filter;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * TraceId过滤器
 */
@Component
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String TRACE_ID_KEY = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            // 从请求头获取traceId，如果没有则生成新的
            String traceId = request.getHeader(TRACE_ID_HEADER);
            if (traceId == null || traceId.isEmpty()) {
                traceId = UUID.randomUUID().toString();
            }
            
            // 将traceId设置到MDC中
            MDC.put(TRACE_ID_KEY, traceId);
            
            // 将traceId设置到响应头中
            response.setHeader(TRACE_ID_HEADER, traceId);
            
            // 继续处理请求
            filterChain.doFilter(request, response);
        } finally {
            // 清除MDC中的traceId
            MDC.remove(TRACE_ID_KEY);
        }
    }

}
