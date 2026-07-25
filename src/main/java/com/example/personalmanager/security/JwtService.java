package com.example.personalmanager.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT 工具服务，负责令牌的生成、解析、用户名提取及有效性校验。
 */
@Component
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMillis;

    // 构造方法注入签名密钥（Base64 编码）与令牌有效期配置
    public JwtService(@Value("${app.jwt.secret}") String base64Secret,
                      @Value("${app.jwt.expiration-ms}") long expirationMillis) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.expirationMillis = expirationMillis;
    }

    /**
     * 为指定用户生成 JWT 令牌。
     *
     * @param userDetails 用户详情
     * @return 签名后的 JWT 字符串
     */
    public String generateToken(UserDetails userDetails) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 从令牌中解析出用户名。
     *
     * @param token JWT 令牌
     * @return 令牌主题（用户名）
     */
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * 校验令牌是否有效（用户名匹配且未过期）。
     *
     * @param token       JWT 令牌
     * @param userDetails 用户详情
     * @return 有效返回 true，否则返回 false
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /**
     * 获取令牌有效期（毫秒）。
     *
     * @return 有效期毫秒数
     */
    public long getExpirationMillis() {
        return expirationMillis;
    }

    // 判断令牌是否已过期
    private boolean isTokenExpired(String token) {
        return parseClaims(token).getExpiration().before(new Date());
    }

    // 使用签名密钥校验并解析令牌的负载声明
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
