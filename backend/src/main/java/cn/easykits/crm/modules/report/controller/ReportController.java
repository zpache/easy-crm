package cn.easykits.crm.modules.report.controller;

import cn.easykits.crm.common.api.Result;
import cn.easykits.crm.modules.report.dto.AchievementQuery;
import cn.easykits.crm.modules.report.dto.DistributionQuery;
import cn.easykits.crm.modules.report.service.ReportServiceImpl;
import cn.easykits.crm.modules.report.vo.AchievementVO;
import cn.easykits.crm.modules.report.vo.DistributionVO;
import cn.easykits.crm.modules.report.vo.FunnelVO;
import cn.easykits.crm.modules.report.vo.RankingVO;
import cn.easykits.crm.modules.report.vo.RevenueVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 报表中心：销售达成 / 销售漏斗 / 销售排行 / 回款收入 / 客户分布。
 * <p>全部为聚合查询，按登录用户的数据范围自动收敛口径。
 */
@Tag(name = "报表中心")
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportServiceImpl reportService;

    @Operation(summary = "销售达成分析（按年月区间/部门/负责人，对比目标与商机、订单实绩）")
    @GetMapping("/achievement")
    @PreAuthorize("hasAuthority('crm:report:achievement')")
    public Result<AchievementVO> achievement(@Valid AchievementQuery query) {
        return Result.ok(reportService.achievement(query));
    }

    @Operation(summary = "销售漏斗（线索→有效线索→商机→赢单→订单，含环节转化率）")
    @GetMapping("/funnel")
    @PreAuthorize("hasAuthority('crm:report:funnel')")
    public Result<FunnelVO> funnel(@Valid AchievementQuery query) {
        return Result.ok(reportService.funnel(query));
    }

    @Operation(summary = "销售排行（负责人维度：赢单/订单/回款，按赢单金额降序）")
    @GetMapping("/ranking")
    @PreAuthorize("hasAuthority('crm:report:ranking')")
    public Result<RankingVO> ranking(@Valid AchievementQuery query) {
        return Result.ok(reportService.ranking(query));
    }

    @Operation(summary = "回款收入分析（按月：计划/实收/已开票/已退款/净流入）")
    @GetMapping("/revenue")
    @PreAuthorize("hasAuthority('crm:report:revenue')")
    public Result<RevenueVO> revenue(@Valid AchievementQuery query) {
        return Result.ok(reportService.revenue(query));
    }

    @Operation(summary = "客户分布（按行业/区域/级别/状态）")
    @GetMapping("/customer-distribution")
    @PreAuthorize("hasAuthority('crm:report:customer')")
    public Result<DistributionVO> customerDistribution(@Valid DistributionQuery query) {
        return Result.ok(reportService.customerDistribution(query));
    }
}
