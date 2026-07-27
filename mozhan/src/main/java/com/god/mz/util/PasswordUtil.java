package com.god.mz.util;

import cn.hutool.crypto.digest.BCrypt;

/**
 * 密码加密工具类，基于 BCrypt 算法
 */
public class PasswordUtil {

    /**
     * 对明文密码进行加密
     * @param rawPassword 明文密码
     * @return 加密后的密文
     */
    public static String encode(String rawPassword) {
        return BCrypt.hashpw(rawPassword);
    }

    /**
     * 校验明文密码是否与密文匹配
     * @param rawPassword     明文密码
     * @param encodedPassword 加密后的密文
     * @return 是否匹配
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        return BCrypt.checkpw(rawPassword, encodedPassword);
    }
}
