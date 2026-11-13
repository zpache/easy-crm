package com.easykits.crm.modules.system.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色视图。
 *
 * @param dataScope  行级数据权限范围：1 本人 / 2 本部门 / 3 本部门及下级 / 4 全部
 * @param permissions 已授权的菜单 ID 列表
 */
public record RoleVO(
        Long id,
        String name,
        String code,
        Integer dataScope,
        Integer sort,
        Integer status,
        String remark,
        LocalDateTime createdAt,
        List<Long> permissions,
        Integer version
) {
}
