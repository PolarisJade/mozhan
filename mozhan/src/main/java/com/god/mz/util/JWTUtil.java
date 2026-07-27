package com.god.mz.util;

import com.god.mz.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;


@Component
public class JWTUtil {
    @Resource
    private JwtProperties jwtProperties;

    private  SecretKey secretKey;
    private  Long expiration;

    @PostConstruct
    public void init(){
        secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        expiration = jwtProperties.getExpiration();
    }

    public String generateToken(Long userId){
        return Jwts.builder()
                //存入用户id
                .claim("userId", userId)
                //设置签名算法
                .signWith(secretKey)
                //设置过期时间
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .compact();
    }

    public Long parseToken(String token){
        Claims claims = Jwts.parser()
                .verifyWith(secretKey) // 设置签名密钥进行校验
                .build()
                .parseSignedClaims(token) // 解析 Token
                .getPayload();
        Object userId = claims.get("userId");
        if (userId == null){
            return null;
        }
        return Long.valueOf(String.valueOf(userId));
    }
}
