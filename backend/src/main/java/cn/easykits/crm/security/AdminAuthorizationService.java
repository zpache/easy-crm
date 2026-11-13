package cn.easykits.crm.security;

import cn.easykits.crm.common.context.LoginUser;
import cn.easykits.crm.entity.SysUser;
import cn.easykits.crm.mapper.SysUserMapper;
import cn.easykits.crm.modules.system.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 请求级授权信息加载：校验用户是否仍启用，并回库取最新的部门、角色与数据范围。
 * 不缓存，保证停用账号、调整角色或数据范围后旧 token 立即生效。
 * <p>TODO（system 模块落地时）：loadAuthorities 追加 sys_role_permission 的权限码。
 */
@Service
@RequiredArgsConstructor
public class AdminAuthorizationService {

    private static final String ROLE_PREFIX = "ROLE_";

    private final SysUserMapper sysUserMapper;
    private final RoleResolver roleResolver;
    private final MenuService menuService;

    /**
     * 用 token 中的用户 ID 回库构造完整 LoginUser；用户被删除或停用时返回 empty。
     */
    public Optional<LoginUser> resolveActiveUser(LoginUser tokenUser) {
        return Optional.ofNullable(buildLoginUser(tokenUser.userId()));
    }

    /**
     * 回库构造完整登录主体（deptId / dataScope 永远取数据库最新值）。
     * 用户不存在或已停用返回 null。
     */
    public LoginUser buildLoginUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
            return null;
        }
        List<String> roles = roleResolver.listRoleCodes(user.getId());
        return new LoginUser(
                user.getId(),
                user.getDeptId(),
                user.getUsername(),
                user.getNickname(),
                roles,
                roleResolver.resolveDataScope(user.getId()));
    }

    /**
     * 角色 authority + 权限码（菜单/按钮上的 auth_code，来自 sys_permission）。
     */
    public List<GrantedAuthority> loadAuthorities(LoginUser loginUser) {
        Set<String> values = new LinkedHashSet<>();
        loginUser.roles().forEach(role -> values.add(ROLE_PREFIX + role));
        values.addAll(menuService.listAuthCodesByUser(loginUser.userId()));
        List<GrantedAuthority> authorities = new ArrayList<>(values.size());
        values.forEach(value -> authorities.add(new SimpleGrantedAuthority(value)));
        return List.copyOf(authorities);
    }
}
