package cn.easykits.crm.modules.report.service;

import cn.easykits.crm.common.exception.BizException;
import cn.easykits.crm.entity.SysUser;
import cn.easykits.crm.mapper.SysUserMapper;
import cn.easykits.crm.modules.report.dto.AchievementQuery;
import cn.easykits.crm.modules.report.dto.DistributionQuery;
import cn.easykits.crm.modules.report.mapper.ReportMapper;
import cn.easykits.crm.modules.report.vo.AchievementVO;
import cn.easykits.crm.modules.report.vo.DistributionVO;
import cn.easykits.crm.modules.report.vo.FunnelVO;
import cn.easykits.crm.modules.report.vo.RankingVO;
import cn.easykits.crm.modules.report.vo.RevenueVO;
import cn.easykits.crm.modules.system.service.DictService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 达成分析：把「销售目标 / 商机赢单实绩 / 订单实绩」三条聚合口径按月份合并。
 * <p>均走 ReportMapper 的聚合 SQL（只用 #{}），不加载业务明细行。
 */
@Service
@RequiredArgsConstructor
public class ReportServiceImpl {

    private final ReportMapper reportMapper;

    private final SysUserMapper sysUserMapper;

    private final DictService dictService;

    public AchievementVO achievement(AchievementQuery query) {
        String startMonth = query.getStartMonth();
        String endMonth = query.getEndMonth();
        // 缺省统计范围：本年 1 月 ~ 当月
        if (startMonth == null) {
            startMonth = YearMonth.now().withMonth(1).toString();
        }
        if (endMonth == null) {
            endMonth = YearMonth.now().toString();
        }
        if (startMonth.compareTo(endMonth) > 0) {
            throw new BizException("起始年月不能晚于结束年月");
        }

        Map<String, AchievementVO.Row> rows = new TreeMap<>();
        merge(rows, reportMapper.targetsByMonth(startMonth, endMonth,
                query.getDeptId(), query.getOwnerId()), RowField.TARGET);
        merge(rows, reportMapper.opportunityActualByMonth(startMonth, endMonth,
                query.getDeptId(), query.getOwnerId()), RowField.OPPORTUNITY);
        merge(rows, reportMapper.orderActualByMonth(startMonth, endMonth,
                query.getDeptId(), query.getOwnerId()), RowField.ORDER);

        BigDecimal totalTarget = BigDecimal.ZERO;
        BigDecimal totalOpportunity = BigDecimal.ZERO;
        BigDecimal totalOrder = BigDecimal.ZERO;
        for (AchievementVO.Row row : rows.values()) {
            row.setAchievementRate(rate(row.getOrderAmount(), row.getTargetAmount()));
            totalTarget = totalTarget.add(row.getTargetAmount());
            totalOpportunity = totalOpportunity.add(row.getOpportunityAmount());
            totalOrder = totalOrder.add(row.getOrderAmount());
        }

        AchievementVO vo = new AchievementVO();
        vo.setRows(new ArrayList<>(rows.values()));
        vo.setTotalTarget(totalTarget);
        vo.setTotalOpportunityAmount(totalOpportunity);
        vo.setTotalOrderAmount(totalOrder);
        vo.setAchievementRate(rate(totalOrder, totalTarget));
        return vo;
    }

    private enum RowField { TARGET, OPPORTUNITY, ORDER }

    private void merge(Map<String, AchievementVO.Row> rows,
                       List<Map<String, Object>> data, RowField field) {
        if (data == null) {
            return;
        }
        for (Map<String, Object> item : data) {
            Object monthObj = item.get("month");
            if (monthObj == null) {
                continue;
            }
            String month = String.valueOf(monthObj);
            AchievementVO.Row row = rows.computeIfAbsent(month, k -> newRow(k));
            BigDecimal amount = toDecimal(item.get("amount"));
            switch (field) {
                case TARGET -> row.setTargetAmount(amount);
                case OPPORTUNITY -> row.setOpportunityAmount(amount);
                case ORDER -> row.setOrderAmount(amount);
            }
        }
    }

    private AchievementVO.Row newRow(String month) {
        AchievementVO.Row row = new AchievementVO.Row();
        row.setMonth(month);
        row.setTargetAmount(BigDecimal.ZERO);
        row.setOpportunityAmount(BigDecimal.ZERO);
        row.setOrderAmount(BigDecimal.ZERO);
        return row;
    }

    private BigDecimal toDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return new BigDecimal(String.valueOf(value));
    }

    /** 达成率 = 实绩 / 目标，保留 4 位小数；目标为 0 返回 null（前端展示 '-'）。 */
    private BigDecimal rate(BigDecimal actual, BigDecimal target) {
        if (target == null || target.signum() == 0) {
            return null;
        }
        return actual.divide(target, 4, RoundingMode.HALF_UP);
    }

    // ============================================================ 销售漏斗

    /** 销售漏斗：线索 → 有效线索 → 商机 → 赢单 → 订单。 */
    public FunnelVO funnel(AchievementQuery query) {
        String startMonth = resolveStartMonth(query.getStartMonth());
        String endMonth = resolveEndMonth(query.getEndMonth());
        assertMonthRange(startMonth, endMonth);
        String startDate = startMonth + "-01";
        String endDate = YearMonth.parse(endMonth).atEndOfMonth().toString();

        Map<String, Object> lead = reportMapper.leadFunnel(startDate, endDate,
                query.getDeptId(), query.getOwnerId());
        Map<String, Object> opp = reportMapper.opportunityFunnel(startDate, endDate,
                query.getDeptId(), query.getOwnerId());
        Map<String, Object> order = reportMapper.orderFunnel(startDate, endDate,
                query.getDeptId(), query.getOwnerId());

        long leadTotal = getLong(lead, "total");
        long leadConverted = getLong(lead, "converted");
        long oppTotal = getLong(opp, "total");
        long wonCount = getLong(opp, "wonCount");
        long orderTotal = getLong(order, "total");

        List<FunnelVO.Stage> stages = new ArrayList<>();
        stages.add(stage("lead", "线索", leadTotal, null, null));
        stages.add(stage("converted", "有效线索（已转化）", leadConverted, null,
                ratio(leadConverted, leadTotal)));
        stages.add(stage("opportunity", "商机", oppTotal, getDecimal(opp, "amount"),
                ratio(oppTotal, leadConverted)));
        stages.add(stage("won", "赢单", wonCount, getDecimal(opp, "wonAmount"),
                ratio(wonCount, oppTotal)));
        stages.add(stage("order", "订单", orderTotal, getDecimal(order, "amount"),
                ratio(orderTotal, wonCount)));

        FunnelVO vo = new FunnelVO();
        vo.setStages(stages);
        return vo;
    }

    private FunnelVO.Stage stage(String code, String label, long count,
                                 BigDecimal amount, BigDecimal conversionRate) {
        FunnelVO.Stage stage = new FunnelVO.Stage();
        stage.setCode(code);
        stage.setLabel(label);
        stage.setCount(count);
        stage.setAmount(amount);
        stage.setConversionRate(conversionRate);
        return stage;
    }

    // ============================================================ 销售排行

    /** 销售排行：负责人维度的赢单 / 订单 / 回款，按赢单金额降序取前 20。 */
    public RankingVO ranking(AchievementQuery query) {
        String startMonth = resolveStartMonth(query.getStartMonth());
        String endMonth = resolveEndMonth(query.getEndMonth());
        assertMonthRange(startMonth, endMonth);
        String startDate = startMonth + "-01";
        String endDate = YearMonth.parse(endMonth).atEndOfMonth().toString();

        Map<Long, RankingVO.Row> rows = new HashMap<>();
        for (Map<String, Object> item : reportMapper.rankingByOwner(startDate, endDate,
                query.getDeptId(), query.getOwnerId())) {
            Long ownerId = toLong(item.get("ownerId"));
            if (ownerId == null) {
                continue;
            }
            RankingVO.Row row = rows.computeIfAbsent(ownerId, id -> {
                RankingVO.Row created = new RankingVO.Row();
                created.setOwnerId(id);
                created.setWonCount(0L);
                created.setOrderCount(0L);
                created.setWonAmount(BigDecimal.ZERO);
                created.setOrderAmount(BigDecimal.ZERO);
                created.setPaymentAmount(BigDecimal.ZERO);
                return created;
            });
            row.setWonCount(getLong(item, "wonCount"));
            row.setWonAmount(getDecimal(item, "wonAmount"));
        }
        for (Map<String, Object> item : reportMapper.orderRankingByOwner(startDate, endDate,
                query.getDeptId(), query.getOwnerId())) {
            Long ownerId = toLong(item.get("ownerId"));
            if (ownerId == null) {
                continue;
            }
            RankingVO.Row row = rows.computeIfAbsent(ownerId, id -> {
                RankingVO.Row created = new RankingVO.Row();
                created.setOwnerId(id);
                created.setWonCount(0L);
                created.setOrderCount(0L);
                created.setWonAmount(BigDecimal.ZERO);
                created.setOrderAmount(BigDecimal.ZERO);
                created.setPaymentAmount(BigDecimal.ZERO);
                return created;
            });
            row.setOrderCount(getLong(item, "orderCount"));
            row.setOrderAmount(getDecimal(item, "orderAmount"));
        }
        for (Map<String, Object> item : reportMapper.paymentRankingByOwner(startDate, endDate,
                query.getDeptId(), query.getOwnerId())) {
            Long ownerId = toLong(item.get("ownerId"));
            if (ownerId == null) {
                continue;
            }
            RankingVO.Row row = rows.computeIfAbsent(ownerId, id -> {
                RankingVO.Row created = new RankingVO.Row();
                created.setOwnerId(id);
                created.setWonCount(0L);
                created.setOrderCount(0L);
                created.setWonAmount(BigDecimal.ZERO);
                created.setOrderAmount(BigDecimal.ZERO);
                created.setPaymentAmount(BigDecimal.ZERO);
                return created;
            });
            row.setPaymentAmount(getDecimal(item, "paymentAmount"));
        }

        List<RankingVO.Row> list = rows.values().stream()
                .sorted(Comparator.comparing(RankingVO.Row::getWonAmount).reversed()
                        .thenComparing(Comparator.comparing(RankingVO.Row::getPaymentAmount).reversed()))
                .limit(20)
                .toList();
        Map<Long, String> names = userNameMap(list.stream().map(RankingVO.Row::getOwnerId).toList());
        int rank = 1;
        for (RankingVO.Row row : list) {
            row.setRank(rank++);
            row.setOwnerName(names.get(row.getOwnerId()));
        }
        RankingVO vo = new RankingVO();
        vo.setRows(list);
        return vo;
    }

    // ============================================================ 回款 / 收入

    /** 回款 / 收入分析：计划、实收、已开票、已退款按月合并。 */
    public RevenueVO revenue(AchievementQuery query) {
        String startMonth = resolveStartMonth(query.getStartMonth());
        String endMonth = resolveEndMonth(query.getEndMonth());
        assertMonthRange(startMonth, endMonth);

        Map<String, RevenueVO.Row> rows = new TreeMap<>();
        fillMonths(rows, startMonth, endMonth);
        mergeAmount(rows, reportMapper.planByMonth(startMonth, endMonth,
                query.getDeptId(), query.getOwnerId()), Field.PLAN);
        mergeAmount(rows, reportMapper.paymentByMonth(startMonth, endMonth,
                query.getDeptId(), query.getOwnerId()), Field.PAYMENT);
        mergeAmount(rows, reportMapper.invoiceByMonth(startMonth, endMonth,
                query.getDeptId(), query.getOwnerId()), Field.INVOICE);
        mergeAmount(rows, reportMapper.refundByMonth(startMonth, endMonth,
                query.getDeptId(), query.getOwnerId()), Field.REFUND);

        BigDecimal plan = BigDecimal.ZERO;
        BigDecimal payment = BigDecimal.ZERO;
        BigDecimal invoice = BigDecimal.ZERO;
        BigDecimal refund = BigDecimal.ZERO;
        BigDecimal net = BigDecimal.ZERO;
        for (RevenueVO.Row row : rows.values()) {
            row.setNetAmount(row.getPaymentAmount().subtract(row.getRefundAmount()));
            row.setCompletionRate(rateDecimals(row.getPaymentAmount(), row.getPlanAmount()));
            plan = plan.add(row.getPlanAmount());
            payment = payment.add(row.getPaymentAmount());
            invoice = invoice.add(row.getInvoiceAmount());
            refund = refund.add(row.getRefundAmount());
            net = net.add(row.getNetAmount());
        }
        RevenueVO vo = new RevenueVO();
        vo.setRows(new ArrayList<>(rows.values()));
        vo.setTotalPlanAmount(plan);
        vo.setTotalPaymentAmount(payment);
        vo.setTotalInvoiceAmount(invoice);
        vo.setTotalRefundAmount(refund);
        vo.setTotalNetAmount(net);
        return vo;
    }

    private enum Field { PLAN, PAYMENT, INVOICE, REFUND }

    private void fillMonths(Map<String, RevenueVO.Row> rows, String startMonth, String endMonth) {
        YearMonth cursor = YearMonth.parse(startMonth);
        YearMonth end = YearMonth.parse(endMonth);
        while (!cursor.isAfter(end)) {
            String key = cursor.toString();
            rows.put(key, emptyRevenueRow(key));
            cursor = cursor.plusMonths(1);
        }
    }

    private RevenueVO.Row emptyRevenueRow(String month) {
        RevenueVO.Row row = new RevenueVO.Row();
        row.setMonth(month);
        row.setPlanAmount(BigDecimal.ZERO);
        row.setPaymentAmount(BigDecimal.ZERO);
        row.setInvoiceAmount(BigDecimal.ZERO);
        row.setRefundAmount(BigDecimal.ZERO);
        row.setNetAmount(BigDecimal.ZERO);
        return row;
    }

    private void mergeAmount(Map<String, RevenueVO.Row> rows, List<Map<String, Object>> data, Field field) {
        if (data == null) {
            return;
        }
        for (Map<String, Object> item : data) {
            Object monthObj = item.get("month");
            if (monthObj == null) {
                continue;
            }
            String month = monthObj.toString();
            RevenueVO.Row row = rows.computeIfAbsent(month, this::emptyRevenueRow);
            BigDecimal amount = getDecimal(item, "amount");
            switch (field) {
                case PLAN -> row.setPlanAmount(amount);
                case PAYMENT -> row.setPaymentAmount(amount);
                case INVOICE -> row.setInvoiceAmount(amount);
                case REFUND -> row.setRefundAmount(amount);
            }
        }
    }

    // ============================================================ 客户分布

    /** 客户分布：按行业 / 区域 / 级别 / 状态维度统计客户数与占比。 */
    public DistributionVO customerDistribution(DistributionQuery query) {
        String dimension = switch (query.getDimension() == null ? "" : query.getDimension()) {
            case "region", "level", "status" -> query.getDimension();
            default -> "industry";
        };
        List<Map<String, Object>> data = reportMapper.customerDistribution(dimension,
                query.getDeptId(), query.getOwnerId());
        long total = data.stream().mapToLong(item -> getLong(item, "total")).sum();

        String dictType = switch (dimension) {
            case "region" -> "customer_region";
            case "level" -> "customer_level";
            case "status" -> "customer_status";
            default -> "customer_industry";
        };
        List<DistributionVO.Item> items = new ArrayList<>();
        for (Map<String, Object> item : data) {
            Object keyObj = item.get("dimValue");
            String key = keyObj == null ? null : keyObj.toString();
            long count = getLong(item, "total");
            DistributionVO.Item vo = new DistributionVO.Item();
            vo.setKey(key);
            vo.setLabel(labelOf(dictType, key));
            vo.setCount(count);
            vo.setPercent(total == 0 ? BigDecimal.ZERO
                    : BigDecimal.valueOf(count).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP));
            items.add(vo);
        }
        DistributionVO vo = new DistributionVO();
        vo.setDimension(dimension);
        vo.setDimensionName(dictTypeLabel(dictType));
        vo.setTotal(total);
        vo.setItems(items);
        return vo;
    }

    private String labelOf(String dictType, String value) {
        if (value == null) {
            return "未填写";
        }
        String label = dictService.labelOf(dictType, value);
        return label == null ? value : label;
    }

    private String dictTypeLabel(String dictType) {
        return switch (dictType) {
            case "customer_region" -> "客户区域";
            case "customer_level" -> "客户级别";
            case "customer_status" -> "客户状态";
            default -> "客户行业";
        };
    }

    // ============================================================ 通用工具

    private Map<Long, String> userNameMap(List<Long> ids) {
        List<Long> distinct = ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Map.of();
        }
        return sysUserMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getNickname));
    }

    private String resolveStartMonth(String startMonth) {
        return startMonth == null ? YearMonth.now().withMonth(1).toString() : startMonth;
    }

    private String resolveEndMonth(String endMonth) {
        return endMonth == null ? YearMonth.now().toString() : endMonth;
    }

    private void assertMonthRange(String startMonth, String endMonth) {
        if (startMonth.compareTo(endMonth) > 0) {
            throw new BizException("起始年月不能晚于结束年月");
        }
    }

    private long getLong(Map<String, Object> data, String key) {
        if (data == null) {
            return 0L;
        }
        Object value = data.get(key);
        return value == null ? 0L : ((Number) value).longValue();
    }

    private BigDecimal getDecimal(Map<String, Object> data, String key) {
        if (data == null) {
            return BigDecimal.ZERO;
        }
        Object value = data.get(key);
        return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
    }

    private Long toLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    /** 数量转化率（%），分母为 0 时返回 null。 */
    private BigDecimal ratio(long numerator, long denominator) {
        if (denominator == 0) {
            return null;
        }
        return BigDecimal.valueOf(numerator).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal rateDecimals(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.signum() == 0) {
            return null;
        }
        return numerator.multiply(BigDecimal.valueOf(100))
                .divide(denominator, 2, RoundingMode.HALF_UP);
    }
}
