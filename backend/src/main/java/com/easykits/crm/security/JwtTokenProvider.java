package com.easykits.crm.security;

import com.easykits.crm.common.context.DataScopeType;
import com.easykits.crm.common.context.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.crypto.SecretKey;

/**
 * JWT 签发与解析。使用 HS256 对称签名。
 * <p>deptId / dataScope 不放进令牌：每次请求由 {@link AdminAuthorizationService}
 * 回库取最新值，保证调整角色数据范围或换部门后立即生效。
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties properties;
    private SecretKey secretKey;

    @PostConstruct
    void init() {
        byte[] keyBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("easy-crm.jwt.secret 长度不足 32 字节，无法用于 HS256 签名");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createToken(LoginUser loginUser) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(loginUser.userId()))
                .issuer(properties.getIssuer())
                .claim("username", loginUser.username())
                .claim("nickname", loginUser.nickname())
                .claim("roles", String.join(",", loginUser.roles()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.getAdminExpire())))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析令牌；签名错误、过期、格式非法均返回 Optional.empty()。
     * 解出的 LoginUser 只含令牌信息，deptId / dataScope 由授权服务回库补齐。
     */
    public Optional<LoginUser> parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .requireIssuer(properties.getIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new LoginUser(
                    Long.valueOf(claims.getSubject()),
                    null,
                    claims.get("username", String.class),
                    claims.get("nickname", String.class),
                    parseRoles(claims),
                    DataScopeType.SELF));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public long getExpireSeconds() {
        return properties.getAdminExpire().toSeconds();
    }

    /**
     * 解析角色（逗号分隔），过滤空白项。
     */
    private List<String> parseRoles(Claims claims) {
        String roles = claims.get("roles", String.class);
        if (roles == null || roles.isBlank()) {
            return List.of();
        }
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .toList();
    }
}
