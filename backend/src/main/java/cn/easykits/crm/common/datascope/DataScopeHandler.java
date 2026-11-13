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
 * 行级数据权限处理器骨架。
 *
 * <p>由 {@code MybatisPlusConfig} 注册到 {@code DataPermissionInterceptor}，
 * 在 SQL 执行前按表逐个回调 {@link #getSqlSegment}：命中注册表且当前用户数据范围
 * 不是「全部」时，向 WHERE 追加 owner_id / dept_id 条件。
 *
 * <p><b>返回值约定（与 MP 3.5.18 源码一致）</b>：返回「需要追加的新条件」，
 * 由拦截器负责 AND 到原 WHERE 上；返回 null 表示该表不追加任何条件。
 * 切勿返回 {@code where AND 新条件}——会导致条件被拼接两次。
 *
 * <p><b>各范围追加的条件</b>（{t} 为表别名，无别名时用表名限定，避免多表 join 歧义）：
 * <ul>
 *   <li>SELF：           {t}.owner_id = ?userId</li>
 *   <li>DEPT：           {t}.dept_id = ?deptId</li>
 *   <li>DEPT_AND_CHILD： {t}.dept_id IN (递归展开 sys_dept 子树)</li>
 *   <li>ALL / 未登录：   不追加条件</li>
 * </ul>
 *
 * <p><b>TODO 清单（认证与业务模块落地时完成）：</b>
 * <ol>
 *   <li>登录态接入后，由 JWT 过滤器将含 deptId、dataScope 的 LoginUser 写入
 *       {@link LoginUserContext}（已完成；未登录时当前放行，上线前必须改为 fail closed）；</li>
 *   <li>DEPT_AND_CHILD 的子树展开 SQL 需按 sys_dept 实际结构确认：现按
 *       「pid 指向父部门」模型编写递归 CTE；若 sys_dept 用 ancestors 路径列，
 *       改为 find_in_set(deptId, ancestors) 的单层子查询即可；</li>
 *   <li>若需要按语句维度豁免（如定时任务、报表跨部门统计），在
 *       {@link #getSqlSegment} 里按 mappedStatementId 增加白名单；</li>
 *   <li>接入新表只需在 {@link DataScopeTableRegistry} 登记，见其类注释。</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataScopeHandler implements MultiDataPermissionHandler {

    private final DataScopeTableRegistry registry;

    @Override
    public Expression getSqlSegment(Table table, Expression where, String mappedStatementId) {
        DataScopeTable config = registry.get(table.getName());
        if (config == null) {
            // 未登记的表不追加条件
            return null;
        }

        LoginUser user = LoginUserContext.get();
        if (user == null || user.dataScope() == DataScopeType.ALL) {
            // TODO：认证接入后未登录场景应改为 fail closed（抛异常），当前骨架阶段放行以便调试
            return null;
        }

        Expression condition = buildCondition(config, user, table);
        if (condition == null) {
            return null;
        }
        // 只返回新条件；拦截器会自行 AND 到原 WHERE 上
        return condition;
    }

    /**
     * 按数据范围构造追加表达式；表缺少对应列或用户缺部门时返回 null（TODO：接入后可改为 fail closed）。
     */
    private Expression buildCondition(DataScopeTable config, LoginUser user, Table table) {
        String prefix = qualifier(table);
        return switch (user.dataScope()) {
            case SELF -> {
                if (config.ownerColumn() == null) {
                    log.warn("数据权限：表 {} 未配置 ownerColumn，SELF 范围过滤跳过", config.tableName());
                    yield null;
                }
                yield parse(prefix + config.ownerColumn() + " = " + user.userId());
            }
            case DEPT -> {
                if (config.deptColumn() == null) {
                    log.warn("数据权限：表 {} 未配置 deptColumn，DEPT 范围过滤跳过", config.tableName());
                    yield null;
                }
                if (user.deptId() == null) {
                    log.warn("数据权限：用户 {} 无部门信息，DEPT 范围过滤跳过", user.userId());
                    yield null;
                }
                yield parse(prefix + config.deptColumn() + " = " + user.deptId());
            }
            case DEPT_AND_CHILD -> {
                if (config.deptColumn() == null) {
                    log.warn("数据权限：表 {} 未配置 deptColumn，DEPT_AND_CHILD 范围过滤跳过", config.tableName());
                    yield null;
                }
                if (user.deptId() == null) {
                    log.warn("数据权限：用户 {} 无部门信息，DEPT_AND_CHILD 范围过滤跳过", user.userId());
                    yield null;
                }
                yield parse(prefix + config.deptColumn() + " IN (" + deptSubtreeSql(user.deptId()) + ")");
            }
            // ALL 在 getSqlSegment 已提前返回
            default -> null;
        };
    }

    /**
     * 列限定符：优先使用别名（如 "c."），无别名时退回表名。
     */
    private String qualifier(Table table) {
        if (table.getAlias() != null && table.getAlias().getName() != null) {
            return table.getAlias().getName() + ".";
        }
        return table.getName() + ".";
    }

    /**
     * 本部门及下级子树 SQL。
     * TODO：按 sys_dept 实际树模型调整（当前假设 pid 列指向父部门，根节点 pid 为 0）。
     * MySQL 8.4 支持递归 CTE；user.deptId 为服务端 Long 值，非外部输入，无注入风险。
     */
    private String deptSubtreeSql(Long deptId) {
        return """
                WITH RECURSIVE dept_tree AS (
                    SELECT id FROM sys_dept WHERE id = %d
                    UNION ALL
                    SELECT d.id FROM sys_dept d JOIN dept_tree t ON d.pid = t.id
                ) SELECT id FROM dept_tree
                """.formatted(deptId);
    }

    /**
     * 将条件字符串解析为 jsqlparser 表达式。
     */
    private Expression parse(String condition) {
        try {
            return CCJSqlParserUtil.parseCondExpression(condition);
        } catch (Exception e) {
            // TODO：接入后建议改为抛出 BizException，fail closed；骨架阶段仅告警
            log.error("数据权限条件解析失败: {}", condition, e);
            return null;
        }
    }
}
