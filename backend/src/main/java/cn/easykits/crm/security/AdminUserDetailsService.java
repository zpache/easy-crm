package cn.easykits.crm.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import cn.easykits.crm.entity.SysUser;
import cn.easykits.crm.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 管理后台账号加载（登录认证阶段）。
 */
@Service
@RequiredArgsConstructor
public class AdminUserDetailsService implements UserDetailsService {

    private final SysUserMapper sysUserMapper;
    private final RoleResolver roleResolver;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = sysUserMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("账号不存在: " + username);
        }
        List<String> roles = roleResolver.listRoleCodes(user.getId());
        return new AdminUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getNickname(),
                roles,
                Integer.valueOf(1).equals(user.getStatus()));
    }
}
