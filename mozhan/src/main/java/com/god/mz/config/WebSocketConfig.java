package com.god.mz.config;

import com.god.mz.handler.ChatWebSocketHandler;
import com.god.mz.interceptor.JwtHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler handler;
    private final JwtHandshakeInterceptor interceptor;

    public WebSocketConfig(ChatWebSocketHandler handler, JwtHandshakeInterceptor interceptor) {
        this.handler = handler;
        this.interceptor = interceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/chatServer")
                .addInterceptors(interceptor)
                .setAllowedOrigins("*");
    }
}
