package cn.easykits.crm.common.datascope;

import cn.easykits.crm.common.context.DataScopeType;
import cn.easykits.crm.common.context.LoginUser;
import cn.easykits.crm.common.context.LoginUserContext;
import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import org.springframework.stereotype.Component;

/**
 * 行级数据权限处理器（fail closed）。
 *
 * <p>由 {@code MybatisPlusConfig} 注册到 {@code DataPermissionInterceptor}，
 * 在 SQL 执行前按表逐个回调 {@link #getSqlSegment}：命中注册表且当前用户数据范围
 * 不是「全部」时，向 WHERE 追加 owner_id / dept_id 条件。
 *
 * <p><b>返回值约定（与 MP 3.5.18 源码一致）</b>：返回「需要追加的新条件」，
 * 由拦截器负责 AND 到原 WHERE 上；返回 null 表示该表不追加任何条件。
 *
 * <p><b>安全约定</b>：用户请求中若缺乏登录上下文、或过滤条件无法构造（缺列、用户无部门、
 * 表达式解析失败），一律追加恒假条件 {@code 1 = 0} 拒绝查询，绝不降级为全表可见。
 * 定时任务等无用户身份的线程必须用 {@link DataScopeContext#runAsSystem(Runnable)} 显式包裹。
 *
 * <p><b>各范围追加的条件</b>（{t} 为表别名，无别名时用表名限定，避免多表 join 歧义）：
 * <ul>
 *   <li>SELF：           {t}.owner_id = ?userId</li>
 *   <li>DEPT：           {t}.dept_id = ?deptId</li>
 *   <li>DEPT_AND_CHILD： {t}.dept_id IN (递归展开 sys_dept 子树)</li>
 *   <li>ALL：            不追加条件</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataScopeHandler implements MultiDataPermissionHandler {

    private static final String DENY_CONDITION = "1 = 0";

    private final DataScopeTableRegistry registry;

    @Override
    public Expression getSqlSegment(Table table, Expression where, String mappedStatementId) {
        DataScopeTable config = registry.get(table.getName());
        if (config == null) {
            return null;
        }
        // 系统内部调用：必须由调用方显式声明，线程级、代码块级豁免
        if (DataScopeContext.isSystemCall()) {
            return null;
        }

        LoginUser user = LoginUserContext.get();
        if (user == null) {
            log.error("数据权限：无登录上下文访问受控表 {}（{}），已拒绝查询", table.getName(), mappedStatementId);
            return deny();
        }
        if (user.dataScope() == DataScopeType.ALL) {
            return null;
        }

        Expression condition = buildCondition(config, user, table);
        if (condition == null) {
            log.error("数据权限：表 {} 无法构造过滤条件（数据范围={}，用户={}），已拒绝查询",
                    table.getName(), user.dataScope(), user.userId());
            return deny();
        }
        return condition;
    }

    /** 恒假条件：任何无法安全判定的场景都退回到「查不到数据」，而不是「查得到全部」。 */
    private Expression deny() {
        try {
            return CCJSqlParserUtil.parseCondExpression(DENY_CONDITION);
        } catch (Exception e) {
            throw new IllegalStateException("数据权限拒绝条件构造失败，应用必须停止执行该查询", e);
        }
    }

    /** 按数据范围构造追加表达式；表缺少对应列或用户缺部门时返回 null，由调用方拒绝查询。 */
    private Expression buildCondition(DataScopeTable config, LoginUser user, Table table) {
        String prefix = qualifier(table);
        return switch (user.dataScope()) {
            case SELF -> {
                if (config.ownerColumn() == null) {
                    log.error("数据权限：表 {} 未配置 ownerColumn，SELF 范围无法过滤", config.tableName());
                    yield null;
                }
                yield parse(prefix + config.ownerColumn() + " = " + user.userId());
            }
            case DEPT -> {
                if (config.deptColumn() == null || user.deptId() == null) {
                    log.error("数据权限：表 {} 缺 deptColumn 或用户无部门，DEPT 范围无法过滤", config.tableName());
                    yield null;
                }
                yield parse(prefix + config.deptColumn() + " = " + user.deptId());
            }
            case DEPT_AND_CHILD -> {
                if (config.deptColumn() == null || user.deptId() == null) {
                    log.error("数据权限：表 {} 缺 deptColumn 或用户无部门，DEPT_AND_CHILD 范围无法过滤",
                            config.tableName());
                    yield null;
                }
                yield parse(prefix + config.deptColumn() + " IN (" + deptSubtreeSql(user.deptId()) + ")");
            }
            default -> null;
        };
    }

    private String qualifier(Table table) {
        if (table.getAlias() != null && table.getAlias().getName() != null) {
            return table.getAlias().getName() + ".";
        }
        return table.getName() + ".";
    }

    /** 本部门及下级子树 SQL；user.deptId 为服务端 Long 值，非外部输入，无注入风险。 */
    private String deptSubtreeSql(Long deptId) {
        return """
                WITH RECURSIVE dept_tree AS (
                    SELECT id FROM sys_dept WHERE id = %d
                    UNION ALL
                    SELECT d.id FROM sys_dept d JOIN dept_tree t ON d.pid = t.id
                ) SELECT id FROM dept_tree
                """.formatted(deptId);
    }

    private Expression parse(String condition) {
        try {
            return CCJSqlParserUtil.parseCondExpression(condition);
        } catch (Exception e) {
            log.error("数据权限条件解析失败: {}", condition, e);
            return null;
        }
    }
}
