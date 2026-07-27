package com.god.mz.config;

import com.god.mz.interceptor.AuthInterceptor;
import com.god.mz.interceptor.JwtInterceptor;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * <p>
 * Web配置类
 * </p>
 * @author ASUS
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Resource
    private JwtInterceptor jwtInterceptor;
    @Resource
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/ws/**");

        registry.addInterceptor(authInterceptor)
                .addPathPatterns(
                        "/article",
                        "/article/update/*",
                        "/article/delete/*",
                        "/article/publish/*",
                        "/article/top/*",
                        "/comment",
                        "/comment/my",
                        "/user/info/update",
                        "/user/password",
                        "/user/logout",
                        "/article-like/*",
                        "/user-follow/*",
                        "/admin/*",
                        "/chat/*",
                        "/diary/**"
                )
                .excludePathPatterns(
                        "/admin/user/login"
                );
    }
}