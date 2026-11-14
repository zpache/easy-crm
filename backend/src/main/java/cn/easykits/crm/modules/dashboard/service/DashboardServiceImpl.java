package cn.easykits.crm.modules.dashboard.service;

import cn.easykits.crm.common.context.LoginUserContext;
import cn.easykits.crm.modules.dashboard.mapper.DashboardMapper;
import cn.easykits.crm.modules.dashboard.vo.DashboardOverviewVO;
import cn.easykits.crm.modules.dashboard.vo.DashboardOverviewVO.FunnelItem;
import cn.easykits.crm.modules.dashboard.vo.DashboardOverviewVO.NewTrendPoint;
import cn.easykits.crm.modules.dashboard.vo.DashboardOverviewVO.OwnerRank;
import cn.easykits.crm.modules.dashboard.vo.DashboardOverviewVO.StageDist;
import cn.easykits.crm.modules.dashboard.vo.DashboardOverviewVO.TrendPoint;
import cn.easykits.crm.modules.lead.constant.LeadStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CRM 数据看板：全部为聚合 SQL（GROUP BY / 标量子查询），查询次数固定（12 次），
 * 不随业务数据量增长；趋势序列按日期补零保证连续；
 * 行级数据权限拦截器自动限定统计口径（SELF 用户看到自己的数据）。
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl {

    private final DashboardMapper dashboardMapper;

    /**
     * 看板总览。trendDays：趋势图统计范围（7/30/90）。
     * 查询次数固定：指标卡 5 + 漏斗 1 + 阶段分布 1 + 趋势 3 + 排行 1 + 提醒 1 = 12 次。
     */
    public DashboardOverviewVO overview(int trendDays) {
        DashboardOverviewVO vo = new DashboardOverviewVO();
        LocalDateTime since7 = LocalDateTime.now().minusDays(7);
        LocalDate startDate = LocalDate.now().minusDays(trendDays - 1L);
        LocalDateTime sinceTrend = startDate.atStartOfDay();

        // ---- 指标卡 ----
        DashboardOverviewVO.Cards cards = new DashboardOverviewVO.Cards();
        cards.setLeadsTotal(dashboardMapper.countLeads());
        cards.setLeadsNew7d(dashboardMapper.countLeadsNew(since7));
        cards.setCustomersTotal(dashboardMapper.countCustomers());
        cards.setCustomersNew7d(dashboardMapper.countCustomersNew(since7));
        Map<String, Object> opp = dashboardMapper.oppStats();
        cards.setOppTotal(toLong(opp.get("totalCnt")));
        cards.setOppOpenCnt(toLong(opp.get("openCnt")));
        cards.setOppOpenAmount(toDecimal(opp.get("openAmount")));
        cards.setOppForecastAmount(toDecimal(opp.get("forecastAmount")));
        Long userId = LoginUserContext.require().userId();
        Map<String, Object> tasks = dashboardMapper.myTaskStats(userId);
        cards.setMyTasksPending(toLong(tasks.get("pendingCnt")));
        cards.setMyTasksOverdue(toLong(tasks.get("overdueCnt")));
        vo.setCards(cards);

        // ---- 销售漏斗：线索状态 → 成交客户 → 商机 ----
        Map<Long, Long> leadByStatus = dashboardMapper.leadStatusCounts().stream()
                .collect(Collectors.toMap(
                        row -> toLong(row.get("k")), row -> toLong(row.get("v"))));
        List<FunnelItem> funnel = new ArrayList<>();
        funnel.add(new FunnelItem("线索总量", cards.getLeadsTotal()));
        funnel.add(new FunnelItem("跟进中/已验证",
                leadByStatus.getOrDefault((long) LeadStatus.FOLLOWING.code(), 0L)
                        + leadByStatus.getOrDefault((long) LeadStatus.VERIFIED.code(), 0L)));
        funnel.add(new FunnelItem("已验证线索",
                leadByStatus.getOrDefault((long) LeadStatus.VERIFIED.code(), 0L)));
        funnel.add(new FunnelItem("成交客户", cards.getCustomersTotal()));
        funnel.add(new FunnelItem("进行中商机", cards.getOppOpenCnt()));
        funnel.add(new FunnelItem("赢单商机", toLong(opp.get("wonCnt"))));
        vo.setFunnel(funnel);

        // ---- 商机阶段分布（进行中） ----
        List<StageDist> dist = new ArrayList<>();
        for (Map<String, Object> row : dashboardMapper.stageDistribution()) {
            StageDist item = new StageDist();
            item.setStageName((String) row.get("stageName"));
            item.setSortNo(toInt(row.get("sortNo")));
            item.setWinRate(toInt(row.get("winRate")));
            item.setCount(toLong(row.get("cnt")));
            item.setAmount(toDecimal(row.get("amount")));
            item.setForecastAmount(toDecimal(row.get("forecastAmount")));
            dist.add(item);
        }
        vo.setStageDistribution(dist);

        // ---- 趋势（按日期补零，保证序列连续） ----
        Map<String, Long> followByDate = toCountMap(dashboardMapper.followTrend(sinceTrend));
        Map<String, Long> leadsByDate = toCountMap(dashboardMapper.newLeadsTrend(sinceTrend));
        Map<String, Long> customersByDate = toCountMap(dashboardMapper.newCustomersTrend(sinceTrend));

        List<TrendPoint> followTrend = new ArrayList<>();
        List<NewTrendPoint> newTrend = new ArrayList<>();
        for (int i = 0; i < trendDays; i++) {
            String date = startDate.plusDays(i).toString();
            TrendPoint point = new TrendPoint();
            point.setDate(date);
            point.setCount(followByDate.getOrDefault(date, 0L));
            followTrend.add(point);
            NewTrendPoint newPoint = new NewTrendPoint();
            newPoint.setDate(date);
            newPoint.setLeads(leadsByDate.getOrDefault(date, 0L));
            newPoint.setCustomers(customersByDate.getOrDefault(date, 0L));
            newTrend.add(newPoint);
        }
        vo.setFollowTrend(followTrend);
        vo.setNewTrend(newTrend);

        // ---- 负责人排行 ----
        List<OwnerRank> rank = new ArrayList<>();
        for (Map<String, Object> row : dashboardMapper.ownerRanking()) {
            OwnerRank item = new OwnerRank();
            item.setOwnerName((String) row.get("ownerName"));
            item.setCount(toLong(row.get("cnt")));
            item.setAmount(toDecimal(row.get("amount")));
            rank.add(item);
        }
        vo.setOwnerRanking(rank);

        // ---- 停滞提醒等级分布 ----
        Map<Integer, Long> stale = new HashMap<>();
        for (Map<String, Object> row : dashboardMapper.staleReminderCounts()) {
            stale.put(toInt(row.get("k")), toLong(row.get("v")));
        }
        vo.setStaleReminders(stale);
        return vo;
    }

    // ============================================================ 私有工具

    /** GROUP BY 日期行转「日期 -> 数量」映射（DATE 类型转字符串） */
    private Map<String, Long> toCountMap(List<Map<String, Object>> rows) {
        return rows.stream().collect(Collectors.toMap(
                row -> String.valueOf(row.get("d")),
                row -> row.get("c") == null ? 0L : ((Number) row.get("c")).longValue(),
                (a, b) -> a,
                java.util.LinkedHashMap::new));
    }

    private long toLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    private int toInt(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
    }

    private BigDecimal toDecimal(Object value) {
        return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
    }
}
