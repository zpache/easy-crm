package cn.easykits.crm.modules.report.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 报表中心聚合查询（全部为 GROUP BY 聚合，不加载明细行；参数一律用 #{} 绑定）。
 * <p>三张口径独立聚合后在 Service 层按月份合并：目标、商机赢单实绩、订单实绩。
 * <p>行级数据权限：crm_sales_target / crm_order 若后续注册进 DataScopeTableRegistry，
 * 拦截器会自动注入 owner_id / dept_id 条件；crm_opportunity 已注册（别名 o）。
 * 返回 Map 的 key 为 month / amount。
 */
@Mapper
public interface ReportMapper {

    /** 销售目标按月汇总（可按部门/负责人过滤）。 */
    @Select("""
            <script>
            SELECT target_month AS month, COALESCE(SUM(target_amount), 0) AS amount
            FROM crm_sales_target
            WHERE deleted = 0
              <if test="startMonth != null"> AND target_month &gt;= #{startMonth}</if>
              <if test="endMonth != null"> AND target_month &lt;= #{endMonth}</if>
              <if test="deptId != null"> AND dept_id = #{deptId}</if>
              <if test="ownerId != null"> AND owner_id = #{ownerId}</if>
            GROUP BY target_month
            ORDER BY target_month
            </script>
            """)
    List<Map<String, Object>> targetsByMonth(@Param("startMonth") String startMonth,
                                             @Param("endMonth") String endMonth,
                                             @Param("deptId") Long deptId,
                                             @Param("ownerId") Long ownerId);

    /** 商机赢单实绩：仅赢单阶段，按实际成交月份汇总 actual_amount。 */
    @Select("""
            <script>
            SELECT DATE_FORMAT(o.actual_close_date, '%Y-%m') AS month,
                   COALESCE(SUM(o.actual_amount), 0) AS amount
            FROM crm_opportunity o
            JOIN crm_opportunity_stage s
              ON o.stage_code = s.stage_code AND s.deleted = 0 AND s.is_won = 1
            WHERE o.deleted = 0 AND o.actual_close_date IS NOT NULL
              <if test="startMonth != null"> AND DATE_FORMAT(o.actual_close_date, '%Y-%m') &gt;= #{startMonth}</if>
              <if test="endMonth != null"> AND DATE_FORMAT(o.actual_close_date, '%Y-%m') &lt;= #{endMonth}</if>
              <if test="deptId != null"> AND o.dept_id = #{deptId}</if>
              <if test="ownerId != null"> AND o.owner_id = #{ownerId}</if>
            GROUP BY DATE_FORMAT(o.actual_close_date, '%Y-%m')
            ORDER BY month
            </script>
            """)
    List<Map<String, Object>> opportunityActualByMonth(@Param("startMonth") String startMonth,
                                                       @Param("endMonth") String endMonth,
                                                       @Param("deptId") Long deptId,
                                                       @Param("ownerId") Long ownerId);

    /** 订单实绩：排除已取消，按创建月份汇总 total_amount。 */
    @Select("""
            <script>
            SELECT DATE_FORMAT(create_time, '%Y-%m') AS month,
                   COALESCE(SUM(total_amount), 0) AS amount
            FROM crm_order
            WHERE deleted = 0 AND status &lt;&gt; 9
              <if test="startMonth != null"> AND DATE_FORMAT(create_time, '%Y-%m') &gt;= #{startMonth}</if>
              <if test="endMonth != null"> AND DATE_FORMAT(create_time, '%Y-%m') &lt;= #{endMonth}</if>
              <if test="deptId != null"> AND dept_id = #{deptId}</if>
              <if test="ownerId != null"> AND owner_id = #{ownerId}</if>
            GROUP BY DATE_FORMAT(create_time, '%Y-%m')
            ORDER BY month
            </script>
            """)
    List<Map<String, Object>> orderActualByMonth(@Param("startMonth") String startMonth,
                                                 @Param("endMonth") String endMonth,
                                                 @Param("deptId") Long deptId,
                                                 @Param("ownerId") Long ownerId);
}
