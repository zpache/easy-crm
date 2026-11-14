package cn.easykits.crm.security;

import cn.easykits.crm.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录防爆破：按账号统计连续失败次数，超过阈值临时锁定。
 *
 * <p>单实例内存实现（无 Redis 依赖），进程重启后计数清零；集群部署时应替换为分布式计数。
 * 锁定期间仍写登录日志，便于审计。
 */
@Slf4j
@Service
public class LoginGuardService {

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    @Value("${easy-crm.security.max-failed-attempts:5}")
    private int maxFailedAttempts = 5;

    @Value("${easy-crm.security.lock-minutes:15}")
    private long lockMinutes = 15;

    /** 登录前置检查：仍在锁定期内直接拒绝。 */
    public void assertNotLocked(String username) {
        if (username == null) {
            return;
        }
        String key = normalize(username);
        Attempt attempt = attempts.get(key);
        if (attempt == null || attempt.lockedUntil() == null) {
            return;
        }
        Instant now = Instant.now();
        if (now.isBefore(attempt.lockedUntil())) {
            long seconds = Math.max(1L, attempt.lockedUntil().getEpochSecond() - now.getEpochSecond());
            throw new BizException("登录失败次数过多，账号已临时锁定，请 " + (seconds / 60 + 1) + " 分钟后重试");
        }
        attempts.remove(key);
    }

    /** 登录失败：累加次数，达到阈值写入锁定截止时间。 */
    public void onFailure(String username) {
        if (username == null) {
            return;
        }
        String key = normalize(username);
        attempts.compute(key, (ignored, old) -> {
            int failed = (old == null ? 0 : old.failed()) + 1;
            Instant lockedUntil = null;
            if (failed >= maxFailedAttempts) {
                lockedUntil = Instant.now().plusSeconds(Math.max(1L, lockMinutes) * 60);
                log.warn("登录防爆破：账号 {} 连续失败 {} 次，临时锁定 {} 分钟", username, failed, lockMinutes);
            }
            return new Attempt(failed, lockedUntil);
        });
    }

    /** 登录成功：清空计数。 */
    public void onSuccess(String username) {
        if (username != null) {
            attempts.remove(normalize(username));
        }
    }

    private String normalize(String username) {
        return username.trim().toLowerCase();
    }

    private record Attempt(int failed, Instant lockedUntil) {
    }
}
