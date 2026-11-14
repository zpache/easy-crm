package cn.easykits.crm.modules.report.service;

import cn.easykits.crm.common.exception.BizException;
import cn.easykits.crm.modules.report.dto.AchievementQuery;
import cn.easykits.crm.modules.report.mapper.ReportMapper;
import cn.easykits.crm.modules.report.vo.AchievementVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 达成分析：把「销售目标 / 商机赢单实绩 / 订单实绩」三条聚合口径按月份合并。
 * <p>均走 ReportMapper 的聚合 SQL（只用 #{}），不加载业务明细行。
 */
@Service
@RequiredArgsConstructor
public class ReportServiceImpl {

    private final ReportMapper reportMapper;

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
}
