package cn.easykits.crm.common.datascope;

/**
 * 一张业务表的数据权限配置：决定该表用哪一列做行级过滤。
 *
 * @param tableName   表名（小写，不含别名）
 * @param ownerColumn 拥有者列名（如 owner_id），SELF 范围时使用；表没有该列传 null
 * @param deptColumn  部门列名（如 dept_id），DEPT / DEPT_AND_CHILD 范围时使用；表没有该列传 null
 */
public record DataScopeTable(String tableName, String ownerColumn, String deptColumn) {

    public DataScopeTable {
        tableName = tableName.toLowerCase();
    }

    /**
     * 常规配置：既有 owner_id 又有 dept_id 的业务表（如客户、商机）。
     */
    public static DataScopeTable of(String tableName) {
        return new DataScopeTable(tableName, "owner_id", "dept_id");
    }
}
