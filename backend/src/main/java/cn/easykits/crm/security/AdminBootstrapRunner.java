package cn.easykits.crm.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import cn.easykits.crm.entity.SysUser;
import cn.easykits.crm.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * 首次启动自动更换初始化管理员口令，避免公开的 admin/admin123 默认凭据流入任何环境。
 *
 * <p>仅当 admin 的口令仍等于 {@code init.sql} 中的初始 BCrypt 值时执行；用户改过密码后不再干预，
 * 因此可重复启动、幂等。可通过环境变量 {@code ADMIN_INITIAL_PASSWORD}（至少 8 位）预设初始口令；
 * 未设置时生成随机口令并在启动日志中打印一次。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements ApplicationRunner {

    public static final String LEGACY_ADMIN_HASH =
            "$2y$10$mOxg6OagMn734O9oAia2Ie3OveTlHyXW/rSvjtO./wJMuPyZlq6Hq";

    private static final String ADMIN_USERNAME = "admin";

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        try {
            SysUser admin = sysUserMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                    .eq(SysUser::getUsername, ADMIN_USERNAME)
                    .last("limit 1"));
            if (admin == null || !LEGACY_ADMIN_HASH.equals(admin.getPassword())) {
                return;
            }
            String configured = System.getenv("ADMIN_INITIAL_PASSWORD");
            boolean fromEnv = configured != null && configured.length() >= 8;
            String raw = fromEnv ? configured : randomPassword();
            sysUserMapper.update(null, Wrappers.<SysUser>lambdaUpdate()
                    .eq(SysUser::getId, admin.getId())
                    .set(SysUser::getPassword, passwordEncoder.encode(raw)));
            if (fromEnv) {
                log.warn("检测到初始化管理员默认口令，已按 ADMIN_INITIAL_PASSWORD 更换，请登录后立即修改");
            } else {
                log.warn("检测到初始化管理员默认口令，已自动更换。随机初始密码（仅显示一次，请立即登录并修改）：{}", raw);
            }
        } catch (Exception e) {
            // 数据库尚未初始化时不应阻断应用启动，下次启动仍会重试
            log.warn("管理员初始口令加固未执行（数据库可能尚未初始化）：{}", e.getMessage());
        }
    }

    private String randomPassword() {
        byte[] bytes = new byte[18];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
