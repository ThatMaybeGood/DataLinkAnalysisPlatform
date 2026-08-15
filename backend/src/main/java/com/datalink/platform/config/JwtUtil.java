package com.datalink.platform.config;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具：令牌签发与解析
 */
@Component
public class JwtUtil {

    /** 签名密钥（由配置注入，生产需环境变量覆盖） */
    @Value("${datalink.jwt.secret}")
    private String secret;

    /** 令牌有效期（秒） */
    @Value("${datalink.jwt.expire-seconds}")
    private long expireSeconds;

    /** 依据配置密钥构建 HMAC 签名密钥 */
    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 签发 JWT 令牌
     */
    public String generateToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expireSeconds * 1000))
                .signWith(key())
                .compact();
    }

    /**
     * 解析令牌中的用户名
     */
    public String parseUsername(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * 校验令牌是否有效（过期/签名错误/格式非法均返回 false）
     */
    public boolean validate(String token) {
        try {
            parseUsername(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
