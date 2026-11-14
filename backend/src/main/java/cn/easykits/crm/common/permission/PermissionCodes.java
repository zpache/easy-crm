package cn.easykits.crm.common.permission;

import java.util.Set;

/**
 * 管理端内置权限码。
 * <p>
 * 菜单 / 按钮的授权关系仍以 sys_permission.auth_code + sys_role_permission 为准；这里集中登记代码中
 * 实际校验的权限码，用于超级管理员兜底和前端按钮展示。新增后端校验时必须同步登记，
 * 否则非 ADMIN 角色即使授权也无法通过 authority 校验，ADMIN 也不会默认获得该码。
 */
public final class PermissionCodes {

    public static final Set<String> ALL = Set.of(
            // 工作台
            "dashboard:analytics",

            // 系统管理
            "system:user:list", "system:user:create", "system:user:update",
            "system:user:delete", "system:user:status", "system:user:reset-password",
            "system:role:list", "system:role:create", "system:role:update",
            "system:role:delete", "system:role:status",
            "system:menu:list", "system:menu:create", "system:menu:update", "system:menu:delete",
            "system:dept:list", "system:dept:create", "system:dept:update", "system:dept:delete",
            "system:operlog:list", "system:loginlog:list",

            // 线索管理
            "crm:lead:list", "crm:lead:create", "crm:lead:update",
            "crm:lead:convert", "crm:lead:import", "crm:lead:to-pool", "crm:lead:claim",

            // 客户 / 联系人管理
            "crm:customer:list", "crm:customer:create", "crm:customer:update",
            "crm:customer:merge", "crm:customer:to-pool", "crm:customer:claim",
            "crm:contact:list", "crm:contact:create", "crm:contact:update",
            "crm:contact:delete", "crm:contact:primary",

            // 商机管理
            "crm:opportunity:list", "crm:opportunity:create", "crm:opportunity:update",
            "crm:opportunity:stage", "crm:opportunity:forecast", "crm:opportunity:stage-manage",

            // 跟进记录 / 待办任务
            "crm:follow:list", "crm:follow:create", "crm:follow:update", "crm:follow:delete",
            "crm:task:list", "crm:task:create", "crm:task:update", "crm:task:complete"
    );

    private PermissionCodes() {
    }
}
