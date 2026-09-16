package com.god.mz.interceptor;

import com.god.mz.common.enums.BizCodeEnum;
import com.god.mz.common.enums.UserTypeEnum;
import com.god.mz.domain.po.User;
import com.god.mz.exception.BizException;
import com.god.mz.mapper.UserMapper;
import com.god.mz.util.UserContext;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 后台管理端角色校验。
 * <p>
 * 必须注册在 {@link AuthInterceptor} <b>之后</b>：本类依赖 AuthInterceptor 已经解析完 token
 * 并把 userId 放进了 {@link UserContext}。注册顺序反了的话这里永远拿不到 userId。
 * </p>
 */
@Component
@Slf4j
public class AdminInterceptor implements HandlerInterceptor {

    @Resource
    private UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            // 正常情况到不了这里：AuthInterceptor 挂在同一批路径上，无 token 的请求已经被它拦下了。
            // 留着是为了防止以后有人调整注册顺序时静默放行。
            response.setStatus(401);
            throw new BizException(401, "未登录");
        }

        User user = userMapper.selectById(userId);
        if (user == null || user.getAdmin() != UserTypeEnum.ENABLE) {
            // 这里刻意用 403 而不是 401：后台前端只在 401 时清 token 跳登录，
            // 若返回 401，"不是管理员"会被表现成"登录已过期"，把用户直接踢出登录页。
            response.setStatus(403);
            log.warn("非管理员账号 {} 尝试访问后台接口 {}", userId, request.getRequestURI());
            throw new BizException(BizCodeEnum.NOT_ADMIN);
        }

        return true;
    }
}
