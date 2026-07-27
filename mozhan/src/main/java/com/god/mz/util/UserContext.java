package com.god.mz.util;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * 用户上下文工具类，用于模拟获取当前登录用户
 * 在实际项目中应通过Session、Token或SecurityContext获取
 * </p>
 * @author ASUS
 */
@Slf4j
public class UserContext {

    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> TOKEN_HOLDER = new ThreadLocal<>();

    /**
     * 设置当前用户ID
     * @param userId 用户ID
     */
    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    /**
     * 获取当前用户ID
     * @return 用户ID
     */
    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }


    /**
     * 清除上下文
     */
    public static void clear() {
        USER_ID_HOLDER.remove();
        TOKEN_HOLDER.remove();
    }

    /**
     * 设置当前请求的 Token
     */
    public static void setToken(String token) {
        TOKEN_HOLDER.set(token);
    }

    /**
     * 获取当前请求的 Token
     */
    public static String getToken() {
        return TOKEN_HOLDER.get();
    }
}
