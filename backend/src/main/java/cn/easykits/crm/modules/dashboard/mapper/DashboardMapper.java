package cn.easykits.crm.modules.dashboard.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * CRM 数据看板聚合查询。全部为 GROUP BY / 标量子查询聚合，不加载业务明细行；
 * 行级数据权限拦截器对各业务表自动注入 owner/dept 条件（SELF 用户看到自己的统计口径）。
 */
@Mapper
public interface DashboardMapper {

    // ===== 指标卡 =====

    @Select("SELECT COUNT(*) FROM crm_lead WHERE deleted = 0")
    long countLeads();

    @Select("SELECT COUNT(*) FROM crm_lead WHERE deleted = 0 AND create_time >= #{since}")
    long countLeadsNew(@Param("since") LocalDateTime since);

    @Select("SELECT COUNT(*) FROM crm_customer WHERE deleted = 0")
    long countCustomers();

    @Select("SELECT COUNT(*) FROM crm_customer WHERE deleted = 0 AND create_time >= #{since}")
    long countCustomersNew(@Param("since") LocalDateTime since);

    /**
     * 商机总量 / 进行中 / 赢单 / 进行中金额 / 加权预测：一条聚合 SQL。
     */
    @Select("""
            SELECT COUNT(*)                                                                                                AS totalCnt,
                   COALESCE(SUM(CASE WHEN s.is_won = 1 THEN 1 ELSE 0 END), 0)                                              AS wonCnt,
                   COALESCE(SUM(CASE WHEN s.is_won = 0 AND s.is_lost = 0 THEN 1 ELSE 0 END), 0)                            AS openCnt,
                   COALESCE(SUM(CASE WHEN s.is_won = 0 AND s.is_lost = 0 THEN o.amount END), 0)                            AS openAmount,
                   COALESCE(SUM(CASE WHEN s.is_won = 0 AND s.is_lost = 0 THEN o.amount * s.default_win_rate / 100 END), 0) AS forecastAmount
            FROM crm_opportunity o
            JOIN crm_opportunity_stage s ON o.stage_code = s.stage_code AND s.deleted = 0
            WHERE o.deleted = 0
            """)
    Map<String, Object> oppStats();

    /**
     * 我的待办：未完成数 / 已过期数（过期动态计算）。
     */
    @Select("""
            SELECT COUNT(*)                                                                                              AS pendingCnt,
                   COALESCE(SUM(CASE WHEN due_time < NOW() THEN 1 ELSE 0 END), 0)                                        AS overdueCnt
            FROM crm_task
            WHERE deleted = 0 AND status = 0 AND owner_id = #{userId}
            """)
    Map<String, Object> myTaskStats(@Param("userId") Long userId);

    // ===== 漏斗 / 分布 =====

    /** 线索状态分布（漏斗第一层） */
    @Select("SELECT status AS k, COUNT(*) AS v FROM crm_lead WHERE deleted = 0 GROUP BY status")
    List<Map<String, Object>> leadStatusCounts();

    /** 商机阶段分布（含加权预测，排除停用阶段） */
    @Select("""
            SELECT s.stage_name                                              AS stageName,
                   s.sort_no                                                 AS sortNo,
                   s.default_win_rate                                        AS winRate,
                   COUNT(o.id)                                               AS cnt,
                   COALESCE(SUM(o.amount), 0)                                AS amount,
                   COALESCE(SUM(o.amount * s.default_win_rate / 100), 0)     AS forecastAmount
            FROM crm_opportunity o
            JOIN crm_opportunity_stage s ON o.stage_code = s.stage_code AND s.deleted = 0
            WHERE o.deleted = 0 AND s.enabled = 1 AND s.is_won = 0 AND s.is_lost = 0
            GROUP BY o.stage_code, s.stage_name, s.sort_no, s.default_win_rate
            ORDER BY s.sort_no
            """)
    List<Map<String, Object>> stageDistribution();

    // ===== 趋势 =====

    /** 近 N 天跟进活动趋势（按日） */
    @Select("""
            SELECT DATE(create_time) AS d, COUNT(*) AS c
            FROM crm_follow_record
            WHERE deleted = 0 AND create_time >= #{since}
            GROUP BY DATE(create_time) ORDER BY d
            """)
    List<Map<String, Object>> followTrend(@Param("since") LocalDateTime since);

    /** 近 N 天新增线索趋势（按日） */
    @Select("""
            SELECT DATE(create_time) AS d, COUNT(*) AS c
            FROM crm_lead
            WHERE deleted = 0 AND create_time >= #{since}
            GROUP BY DATE(create_time) ORDER BY d
            """)
    List<Map<String, Object>> newLeadsTrend(@Param("since") LocalDateTime since);

    /** 近 N 天新增客户趋势（按日） */
    @Select("""
            SELECT DATE(create_time) AS d, COUNT(*) AS c
            FROM crm_customer
            WHERE deleted = 0 AND create_time >= #{since}
            GROUP BY DATE(create_time) ORDER BY d
            """)
    List<Map<String, Object>> newCustomersTrend(@Param("since") LocalDateTime since);

    // ===== 排行 =====

    /**
     * 负责人进行中商机 TOP 5（金额降序）。
     */
    @Select("""
            SELECT u.nickname AS ownerName, COUNT(o.id) AS cnt,
                   COALESCE(SUM(o.amount), 0) AS amount
            FROM crm_opportunity o
            JOIN sys_user u ON o.owner_id = u.id AND u.deleted = 0
            JOIN crm_opportunity_stage s ON o.stage_code = s.stage_code AND s.deleted = 0
            WHERE o.deleted = 0 AND s.is_won = 0 AND s.is_lost = 0
            GROUP BY o.owner_id, u.nickname
            ORDER BY amount DESC LIMIT 5
            """)
    List<Map<String, Object>> ownerRanking();

    // ===== 提醒 / 停滞 =====

    /** 未读停滞提醒按等级聚合（1 黄 2 橙 3 红） */
    @Select("""
            SELECT remind_level AS k, COUNT(*) AS v
            FROM crm_reminder
            WHERE deleted = 0 AND read_flag = 0 AND biz_type = 'opportunity_stale'
            GROUP BY remind_level
            """)
    List<Map<String, Object>> staleReminderCounts();
}
