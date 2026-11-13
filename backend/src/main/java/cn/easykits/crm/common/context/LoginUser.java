package cn.easykits.crm.common.context;

import java.util.List;

/**
 * 当前登录主体。
 *
 * <p>TODO（下一轮接入认证时补齐）：由 JWT 认证过滤器解析 token 后构造并写入
 * {@link LoginUserContext}。dataScope 取值为「当前用户全部角色中数据范围最大者」，
 * 由 RBAC 模块查询 sys_user_role -> sys_role.data_scope 后合并得出。
 *
 * @param userId    用户 ID（sys_user.id）
 * @param deptId    所属部门 ID（sys_user.dept_id）
 * @param username  登录账号
 * @param nickname  昵称
 * @param roles     角色 key 列表，如 ["admin"]；为空表示未分配角色
 * @param dataScope 行级数据权限范围（本人 / 本部门 / 本部门及下级 / 全部）
 */
public record LoginUser(Long userId,
                        Long deptId,
                        String username,
                        String nickname,
                        List<String> roles,
                        DataScopeType dataScope) {

    public LoginUser {
        roles = roles == null ? List.of() : List.copyOf(roles);
        dataScope = dataScope == null ? DataScopeType.SELF : dataScope;
    }
}
