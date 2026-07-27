package com.god.mz.interceptor;

import cn.hutool.core.util.StrUtil;
import com.god.mz.common.constant.RedisConstant;
import com.god.mz.util.JWTUtil;
import com.god.mz.util.UserContext;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
public class JwtInterceptor implements HandlerInterceptor {
    @Resource
    private JWTUtil jwtUtil;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        String token = request.getHeader("Authorization");

        if(StrUtil.isNotBlank(token)){
            if(token.startsWith("Bearer ")){
                token = token.substring(7);
            }

            try {
                // 检查 token 是否在黑名单中（已退出登录）
                String blacklistKey = RedisConstant.TOKEN_BLACKLIST_PREFIX + token;
                if (stringRedisTemplate.hasKey(blacklistKey)) {
                    log.debug("token已失效，作为未登录用户处理");
                } else {
                    Long userId = jwtUtil.parseToken(token);
                    if (userId != null) {
                        UserContext.setUserId(userId);
                        UserContext.setToken(token);
                        log.debug("用户{}认证成功", userId);
                    }
                }
            } catch (Exception e) {
                log.debug("token解析失败，作为未登录用户处理: {}", e.getMessage());
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, Exception ex) {
        UserContext.clear();
    }
}
