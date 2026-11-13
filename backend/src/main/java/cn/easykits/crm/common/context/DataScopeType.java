package cn.easykits.crm.common.context;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 行级数据权限范围（与 RBAC 中的数据范围授权对应）。
 *
 * <ul>
 *   <li>{@link #SELF}          仅本人：可见 owner_id = 当前用户的数据</li>
 *   <li>{@link #DEPT}          本部门：可见 dept_id = 当前用户部门的数据</li>
 *   <li>{@link #DEPT_AND_CHILD} 本部门及下级：可见 dept_id ∈ 当前部门及其全部下级部门的数据</li>
 *   <li>{@link #ALL}           全部：不追加任何过滤条件</li>
 * </ul>
 */
@Getter
@RequiredArgsConstructor
public enum DataScopeType {

    SELF("仅本人"),
    DEPT("本部门"),
    DEPT_AND_CHILD("本部门及下级"),
    ALL("全部");

    private final String label;

    /**
     * 由角色授权存储的数字值还原枚举（数据库约定：1 本人 / 2 本部门 / 3 本部门及下级 / 4 全部）。
     * 未知值按最严格的 SELF 处理，保证权限兜底。
     */
    public static DataScopeType of(Integer value) {
        if (value == null) {
            return SELF;
        }
        return switch (value) {
            case 1 -> SELF;
            case 2 -> DEPT;
            case 3 -> DEPT_AND_CHILD;
            case 4 -> ALL;
            default -> SELF;
        };
    }
}
