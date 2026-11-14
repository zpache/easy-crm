package cn.easykits.crm.modules.dashboard.controller;

import cn.easykits.crm.common.api.Result;
import cn.easykits.crm.modules.dashboard.service.DashboardServiceImpl;
import cn.easykits.crm.modules.dashboard.vo.DashboardOverviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * CRM 数据看板（分析页）。
 * 统计口径随行级数据权限自动限定：SELF 用户看到自己的数据，ALL 用户看到全局。
 */
@Tag(name = "数据看板")
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardServiceImpl dashboardService;

    @Operation(summary = "看板总览（指标卡 / 漏斗 / 阶段分布 / 趋势 / 排行 / 停滞提醒）")
    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('dashboard:analytics')")
    public Result<DashboardOverviewVO> overview(
            @RequestParam(defaultValue = "30") int trendDays) {
        return Result.ok(dashboardService.overview(trendDays));
    }
}
