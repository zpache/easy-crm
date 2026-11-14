package cn.easykits.crm.modules.report.controller;

import cn.easykits.crm.common.api.Result;
import cn.easykits.crm.modules.report.dto.AchievementQuery;
import cn.easykits.crm.modules.report.service.ReportServiceImpl;
import cn.easykits.crm.modules.report.vo.AchievementVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 报表中心：销售达成分析。
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
}
