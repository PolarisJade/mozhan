package com.god.mz.config;

import com.god.mz.interceptor.AdminInterceptor;
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
    @Resource
    private AdminInterceptor adminInterceptor;

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
                        // 必须用 /**：Spring Boot 3 的 PathPattern 里 * 只匹配一级路径，
                        // 写成 /admin/* 时 /admin/user/page 这类两级路径根本不会被拦到。
                        "/admin/**",
                        "/chat/*",
                        "/diary/**",
                        "/ai/**"
                )
                .excludePathPatterns(
                        "/admin/user/login"
                );

        // 注册顺序不能变：adminInterceptor 依赖 authInterceptor 先把 userId 放进 UserContext
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/user/login");
    }
}