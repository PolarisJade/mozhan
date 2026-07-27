package com.god.mz.interceptor;

import cn.hutool.core.util.StrUtil;
import com.god.mz.common.constant.RedisConstant;
import com.god.mz.exception.BizException;
import com.god.mz.util.JWTUtil;
import com.god.mz.util.UserContext;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
public class AuthInterceptor implements HandlerInterceptor {
    @Resource
    private JWTUtil jwtUtil;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        String token = request.getHeader("Authorization");
        if(StrUtil.isBlank(token)){
            response.setStatus(401);
            log.info("token为空，拦截请求");
            throw new BizException(401, "token为空");
        }
        // 移除Bearer前缀
        if(token.startsWith("Bearer ")){
            token = token.substring(7);
        }

        Long userId;
        try {
            // 检查 token 是否在黑名单中（已退出登录）
            String blacklistKey = RedisConstant.TOKEN_BLACKLIST_PREFIX + token;
            if (stringRedisTemplate.hasKey(blacklistKey)) {
                response.setStatus(401);
                log.info("token已失效（在黑名单中）");
                throw new BizException(401, "token已失效，请重新登录");
            }
            userId = jwtUtil.parseToken(token);
        } catch (Exception e) {
            response.setStatus(401);
            log.info("token解析失败: {}", e.getMessage());
            throw new BizException(401, "token解析失败");
        }

        if(userId == null){
            response.setStatus(401);
            log.info("token无效!");
            throw new BizException(401, "token无效");
        }

        UserContext.setUserId(userId);
        UserContext.setToken(token);
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, @Nullable Exception ex) {
        UserContext.clear();
    }
}
