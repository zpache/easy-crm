package cn.easykits.crm.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import cn.easykits.crm.common.context.DataScopeType;
import cn.easykits.crm.entity.SysRole;
import cn.easykits.crm.entity.SysUserRole;
import cn.easykits.crm.mapper.SysRoleMapper;
import cn.easykits.crm.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 角色查询：登录认证与请求级授权共用。
 * <p>TODO（system 模块完整落地时）：角色缓存、按角色兜底默认数据范围等策略。
 */
@Component
@RequiredArgsConstructor
public class RoleResolver {

    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;

    /**
     * 用户当前启用的角色编码列表（sys_user_role -> sys_role，按 sort 升序）。
     */
    public List<String> listRoleCodes(Long userId) {
        return listActiveRoles(userId).stream().map(SysRole::getCode).toList();
    }

    /**
     * 用户的数据范围：取全部启用角色中范围最大的（数值越大范围越广，见 DataScopeType.of）。
     * 未分配角色时为 SELF（最严格兜底）。
     */
    public DataScopeType resolveDataScope(Long userId) {
        return listActiveRoles(userId).stream()
                .map(SysRole::getDataScope)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .map(DataScopeType::of)
                .orElse(DataScopeType.SELF);
    }

    private List<SysRole> listActiveRoles(Long userId) {
        List<Long> roleIds = sysUserRoleMapper.selectList(
                        Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).distinct().toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return sysRoleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
                .in(SysRole::getId, roleIds)
                .eq(SysRole::getStatus, 1)
                .orderByAsc(SysRole::getSort));
    }
}
