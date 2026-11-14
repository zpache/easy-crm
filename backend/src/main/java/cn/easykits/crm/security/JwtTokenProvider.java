package cn.easykits.crm.security;

import cn.easykits.crm.common.context.DataScopeType;
import cn.easykits.crm.common.context.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.crypto.SecretKey;

/**
 * JWT 签发与解析（HS256）。
 *
 * <p>access token 与 refresh token 分开签发：refresh token 带 {@code typ=refresh} 且有效期更长，
 * 只能用于 {@code /auth/refresh} 换取新的 access token；两者都携带 {@code ver}（用户 tokenVersion），
 * 改密 / 重置密码 / 登出 / 停用时递增版本即可让全部旧令牌立即失效。
 *
 * <p>deptId / dataScope 不放进令牌：每次请求由 {@link AdminAuthorizationService}
 * 回库取最新值，保证调整角色数据范围或换部门后立即生效。
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final String CLAIM_VER = "ver";
    private static final String CLAIM_TYPE = "typ";
    private static final String TYPE_REFRESH = "refresh";

    private final JwtProperties properties;
    private SecretKey secretKey;

    @PostConstruct
    void init() {
        String secret = properties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "必须配置 easy-crm.jwt.secret（环境变量 JWT_SECRET），拒绝使用内置默认密钥启动");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("easy-crm.jwt.secret 长度不足 32 字节，无法用于 HS256 签名");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createAccessToken(LoginUser loginUser, long tokenVersion) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(loginUser.userId()))
                .issuer(properties.getIssuer())
                .claim(CLAIM_VER, tokenVersion)
                .claim("username", loginUser.username())
                .claim("nickname", loginUser.nickname())
                .claim("roles", String.join(",", loginUser.roles()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.getAdminExpire())))
                .signWith(secretKey)
                .compact();
    }

    public String createRefreshToken(Long userId, long tokenVersion) {
        Instant now = Instant.now();
        Duration expire = properties.getRefreshExpire() == null
                ? Duration.ofDays(7) : properties.getRefreshExpire();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuer(properties.getIssuer())
                .claim(CLAIM_VER, tokenVersion)
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expire)))
                .signWith(secretKey)
                .compact();
    }

    /** 解析 access token；签名错误、过期、格式非法、误传 refresh token 均返回 empty。 */
    public Optional<LoginUser> parseToken(String token) {
        try {
            Claims claims = parseClaims(token);
            if (TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class))) {
                return Optional.empty();
            }
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

    /** 读取 access token 中的 tokenVersion；解析失败返回 -1（与数据库中任何版本都不相等）。 */
    public long parseTokenVersion(String token) {
        try {
            Number ver = parseClaims(token).get(CLAIM_VER, Number.class);
            return ver == null ? -1L : ver.longValue();
        } catch (JwtException | IllegalArgumentException e) {
            return -1L;
        }
    }

    /** 解析 refresh token，校验其类型标记，防止 access token 被当作 refresh token 使用。 */
    public Optional<RefreshPayload> parseRefreshToken(String token) {
        try {
            Claims claims = parseClaims(token);
            if (!TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class))) {
                return Optional.empty();
            }
            Number ver = claims.get(CLAIM_VER, Number.class);
            return Optional.of(new RefreshPayload(
                    Long.valueOf(claims.getSubject()),
                    ver == null ? -1L : ver.longValue()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public long getExpireSeconds() {
        return properties.getAdminExpire().toSeconds();
    }

    public record RefreshPayload(Long userId, long tokenVersion) {
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(properties.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

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
