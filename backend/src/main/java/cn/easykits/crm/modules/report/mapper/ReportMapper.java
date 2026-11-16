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

    // ============================================================ 销售漏斗

    /** 线索漏斗：总数 / 已转化数（转化以 converted_customer_id 非空判定）。 */
    @Select("""
            <script>
            SELECT COUNT(*) AS total,
                   COALESCE(SUM(CASE WHEN converted_customer_id IS NOT NULL THEN 1 ELSE 0 END), 0) AS converted
            FROM crm_lead
            WHERE deleted = 0
              <if test="startDate != null"> AND DATE(create_time) &gt;= #{startDate}</if>
              <if test="endDate != null"> AND DATE(create_time) &lt;= #{endDate}</if>
              <if test="deptId != null"> AND dept_id = #{deptId}</if>
              <if test="ownerId != null"> AND owner_id = #{ownerId}</if>
            </script>
            """)
    Map<String, Object> leadFunnel(@Param("startDate") String startDate,
                                   @Param("endDate") String endDate,
                                   @Param("deptId") Long deptId,
                                   @Param("ownerId") Long ownerId);

    /** 商机漏斗：全部商机数与金额 / 赢单数与赢单金额。 */
    @Select("""
            <script>
            SELECT COUNT(*) AS total,
                   COALESCE(SUM(o.amount), 0) AS amount,
                   COALESCE(SUM(CASE WHEN s.is_won = 1 THEN 1 ELSE 0 END), 0) AS wonCount,
                   COALESCE(SUM(CASE WHEN s.is_won = 1 THEN o.actual_amount ELSE 0 END), 0) AS wonAmount
            FROM crm_opportunity o
            LEFT JOIN crm_opportunity_stage s
              ON o.stage_code = s.stage_code AND s.deleted = 0
            WHERE o.deleted = 0
              <if test="startDate != null"> AND DATE(o.create_time) &gt;= #{startDate}</if>
              <if test="endDate != null"> AND DATE(o.create_time) &lt;= #{endDate}</if>
              <if test="deptId != null"> AND o.dept_id = #{deptId}</if>
              <if test="ownerId != null"> AND o.owner_id = #{ownerId}</if>
            </script>
            """)
    Map<String, Object> opportunityFunnel(@Param("startDate") String startDate,
                                          @Param("endDate") String endDate,
                                          @Param("deptId") Long deptId,
                                          @Param("ownerId") Long ownerId);

    /** 订单漏斗：有效订单数与金额（排除已取消 status=9）。 */
    @Select("""
            <script>
            SELECT COUNT(*) AS total, COALESCE(SUM(total_amount), 0) AS amount
            FROM crm_order
            WHERE deleted = 0 AND status &lt;&gt; 9
              <if test="startDate != null"> AND DATE(create_time) &gt;= #{startDate}</if>
              <if test="endDate != null"> AND DATE(create_time) &lt;= #{endDate}</if>
              <if test="deptId != null"> AND dept_id = #{deptId}</if>
              <if test="ownerId != null"> AND owner_id = #{ownerId}</if>
            </script>
            """)
    Map<String, Object> orderFunnel(@Param("startDate") String startDate,
                                    @Param("endDate") String endDate,
                                    @Param("deptId") Long deptId,
                                    @Param("ownerId") Long ownerId);

    // ============================================================ 销售排行

    /** 负责人赢单排行（仅赢单阶段）。 */
    @Select("""
            <script>
            SELECT o.owner_id AS ownerId, COUNT(*) AS wonCount,
                   COALESCE(SUM(o.actual_amount), 0) AS wonAmount
            FROM crm_opportunity o
            JOIN crm_opportunity_stage s
              ON o.stage_code = s.stage_code AND s.deleted = 0 AND s.is_won = 1
            WHERE o.deleted = 0 AND o.owner_id IS NOT NULL
              <if test="startDate != null"> AND DATE(o.actual_close_date) &gt;= #{startDate}</if>
              <if test="endDate != null"> AND DATE(o.actual_close_date) &lt;= #{endDate}</if>
              <if test="deptId != null"> AND o.dept_id = #{deptId}</if>
              <if test="ownerId != null"> AND o.owner_id = #{ownerId}</if>
            GROUP BY o.owner_id
            </script>
            """)
    List<Map<String, Object>> rankingByOwner(@Param("startDate") String startDate,
                                             @Param("endDate") String endDate,
                                             @Param("deptId") Long deptId,
                                             @Param("ownerId") Long ownerId);

    /** 负责人订单金额排行。 */
    @Select("""
            <script>
            SELECT o.owner_id AS ownerId, COUNT(*) AS orderCount,
                   COALESCE(SUM(o.total_amount), 0) AS orderAmount
            FROM crm_order o
            WHERE o.deleted = 0 AND o.status &lt;&gt; 9 AND o.owner_id IS NOT NULL
              <if test="startDate != null"> AND DATE(o.create_time) &gt;= #{startDate}</if>
              <if test="endDate != null"> AND DATE(o.create_time) &lt;= #{endDate}</if>
              <if test="deptId != null"> AND o.dept_id = #{deptId}</if>
              <if test="ownerId != null"> AND o.owner_id = #{ownerId}</if>
            GROUP BY o.owner_id
            </script>
            """)
    List<Map<String, Object>> orderRankingByOwner(@Param("startDate") String startDate,
                                                  @Param("endDate") String endDate,
                                                  @Param("deptId") Long deptId,
                                                  @Param("ownerId") Long ownerId);

    /** 负责人回款金额排行（回款经订单归属到负责人）。 */
    @Select("""
            <script>
            SELECT o.owner_id AS ownerId, COALESCE(SUM(p.amount), 0) AS paymentAmount
            FROM crm_payment p
            JOIN crm_order o ON o.id = p.order_id AND o.deleted = 0
            WHERE p.deleted = 0 AND o.owner_id IS NOT NULL
              <if test="startDate != null"> AND DATE(p.payment_date) &gt;= #{startDate}</if>
              <if test="endDate != null"> AND DATE(p.payment_date) &lt;= #{endDate}</if>
              <if test="deptId != null"> AND o.dept_id = #{deptId}</if>
              <if test="ownerId != null"> AND o.owner_id = #{ownerId}</if>
            GROUP BY o.owner_id
            </script>
            """)
    List<Map<String, Object>> paymentRankingByOwner(@Param("startDate") String startDate,
                                                    @Param("endDate") String endDate,
                                                    @Param("deptId") Long deptId,
                                                    @Param("ownerId") Long ownerId);

    // ============================================================ 回款 / 收入分析

    /** 回款计划按月汇总。 */
    @Select("""
            <script>
            SELECT DATE_FORMAT(pl.plan_date, '%Y-%m') AS month,
                   COALESCE(SUM(pl.plan_amount), 0) AS amount
            FROM crm_payment_plan pl
            JOIN crm_order o ON o.id = pl.order_id AND o.deleted = 0
            WHERE pl.deleted = 0 AND pl.plan_date IS NOT NULL
              <if test="startMonth != null"> AND DATE_FORMAT(pl.plan_date, '%Y-%m') &gt;= #{startMonth}</if>
              <if test="endMonth != null"> AND DATE_FORMAT(pl.plan_date, '%Y-%m') &lt;= #{endMonth}</if>
              <if test="deptId != null"> AND o.dept_id = #{deptId}</if>
              <if test="ownerId != null"> AND o.owner_id = #{ownerId}</if>
            GROUP BY DATE_FORMAT(pl.plan_date, '%Y-%m')
            ORDER BY month
            </script>
            """)
    List<Map<String, Object>> planByMonth(@Param("startMonth") String startMonth,
                                          @Param("endMonth") String endMonth,
                                          @Param("deptId") Long deptId,
                                          @Param("ownerId") Long ownerId);

    /** 实收回款按月汇总。 */
    @Select("""
            <script>
            SELECT DATE_FORMAT(p.payment_date, '%Y-%m') AS month,
                   COALESCE(SUM(p.amount), 0) AS amount
            FROM crm_payment p
            JOIN crm_order o ON o.id = p.order_id AND o.deleted = 0
            WHERE p.deleted = 0 AND p.payment_date IS NOT NULL
              <if test="startMonth != null"> AND DATE_FORMAT(p.payment_date, '%Y-%m') &gt;= #{startMonth}</if>
              <if test="endMonth != null"> AND DATE_FORMAT(p.payment_date, '%Y-%m') &lt;= #{endMonth}</if>
              <if test="deptId != null"> AND o.dept_id = #{deptId}</if>
              <if test="ownerId != null"> AND o.owner_id = #{ownerId}</if>
            GROUP BY DATE_FORMAT(p.payment_date, '%Y-%m')
            ORDER BY month
            </script>
            """)
    List<Map<String, Object>> paymentByMonth(@Param("startMonth") String startMonth,
                                             @Param("endMonth") String endMonth,
                                             @Param("deptId") Long deptId,
                                             @Param("ownerId") Long ownerId);

    /** 已开票金额按月汇总（status = 1 已开票）。 */
    @Select("""
            <script>
            SELECT DATE_FORMAT(i.apply_date, '%Y-%m') AS month,
                   COALESCE(SUM(i.amount), 0) AS amount
            FROM crm_invoice i
            JOIN crm_order o ON o.id = i.order_id AND o.deleted = 0
            WHERE i.deleted = 0 AND i.status = 1 AND i.apply_date IS NOT NULL
              <if test="startMonth != null"> AND DATE_FORMAT(i.apply_date, '%Y-%m') &gt;= #{startMonth}</if>
              <if test="endMonth != null"> AND DATE_FORMAT(i.apply_date, '%Y-%m') &lt;= #{endMonth}</if>
              <if test="deptId != null"> AND o.dept_id = #{deptId}</if>
              <if test="ownerId != null"> AND o.owner_id = #{ownerId}</if>
            GROUP BY DATE_FORMAT(i.apply_date, '%Y-%m')
            ORDER BY month
            </script>
            """)
    List<Map<String, Object>> invoiceByMonth(@Param("startMonth") String startMonth,
                                             @Param("endMonth") String endMonth,
                                             @Param("deptId") Long deptId,
                                             @Param("ownerId") Long ownerId);

    /** 已退款金额按月汇总（status = 2 已退款）。 */
    @Select("""
            <script>
            SELECT DATE_FORMAT(r.refund_date, '%Y-%m') AS month,
                   COALESCE(SUM(r.amount), 0) AS amount
            FROM crm_refund r
            JOIN crm_order o ON o.id = r.order_id AND o.deleted = 0
            WHERE r.deleted = 0 AND r.status = 2 AND r.refund_date IS NOT NULL
              <if test="startMonth != null"> AND DATE_FORMAT(r.refund_date, '%Y-%m') &gt;= #{startMonth}</if>
              <if test="endMonth != null"> AND DATE_FORMAT(r.refund_date, '%Y-%m') &lt;= #{endMonth}</if>
              <if test="deptId != null"> AND o.dept_id = #{deptId}</if>
              <if test="ownerId != null"> AND o.owner_id = #{ownerId}</if>
            GROUP BY DATE_FORMAT(r.refund_date, '%Y-%m')
            ORDER BY month
            </script>
            """)
    List<Map<String, Object>> refundByMonth(@Param("startMonth") String startMonth,
                                            @Param("endMonth") String endMonth,
                                            @Param("deptId") Long deptId,
                                            @Param("ownerId") Long ownerId);

    // ============================================================ 客户分布

    /** 客户按维度分布（dimension 仅接受 industry / region / level / status，由 Service 白名单校验）。 */
    @Select("""
            <script>
            SELECT
              <choose>
                <when test="dimension == 'region'">region</when>
                <when test="dimension == 'level'">level</when>
                <when test="dimension == 'status'">status</when>
                <otherwise>industry</otherwise>
              </choose>
              AS dimValue,
              COUNT(*) AS total
            FROM crm_customer
            WHERE deleted = 0 AND merged_to_id IS NULL
              <if test="deptId != null"> AND dept_id = #{deptId}</if>
              <if test="ownerId != null"> AND owner_id = #{ownerId}</if>
            GROUP BY dimValue
            ORDER BY total DESC
            </script>
            """)
    List<Map<String, Object>> customerDistribution(@Param("dimension") String dimension,
                                                   @Param("deptId") Long deptId,
                                                   @Param("ownerId") Long ownerId);
}
