package com.god.mz.interceptor;

import com.god.mz.util.JWTUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket握手拦截器 - 验证JWT Token
 */
@Slf4j
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    @Resource
    private JWTUtil jwtUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        // 从URL参数中获取token
        String token = extractTokenFromRequest(request);

        if (token == null || token.isEmpty()) {
            log.warn("WebSocket握手失败: 缺少token");
            return false;
        }

        // 解析token获取用户ID
        Long userId = jwtUtil.parseToken(token);
        if (userId == null) {
            log.warn("WebSocket握手失败: token无效");
            return false;
        }

        // 将用户ID存入attributes，供后续使用
        attributes.put("userId", userId);
        log.debug("WebSocket握手成功, 用户ID: {}", userId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 握手后的处理，这里不需要额外操作
    }

    /**
     * 从请求中提取token
     *
     * @param request HTTP请求
     * @return token字符串
     */
    private String extractTokenFromRequest(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            // 从URL参数中获取
            String token = servletRequest.getServletRequest().getParameter("token");
            if (token != null && !token.isEmpty()) {
                return token;
            }

            // 从Header中获取
            String authHeader = servletRequest.getServletRequest().getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
        }
        return null;
    }
}
