package cn.easykits.crm.modules.opportunity.controller;

import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.Result;
import cn.easykits.crm.modules.opportunity.dto.ForecastQuery;
import cn.easykits.crm.modules.opportunity.dto.OpportunityCreateForm;
import cn.easykits.crm.modules.opportunity.dto.OpportunityQuery;
import cn.easykits.crm.modules.opportunity.dto.OpportunityUpdateForm;
import cn.easykits.crm.modules.opportunity.dto.StageChangeForm;
import cn.easykits.crm.modules.opportunity.dto.StageForm;
import cn.easykits.crm.modules.opportunity.service.OpportunityServiceImpl;
import cn.easykits.crm.modules.opportunity.vo.BoardStage;
import cn.easykits.crm.modules.opportunity.vo.ForecastVO;
import cn.easykits.crm.modules.opportunity.vo.OpportunityVO;
import cn.easykits.crm.modules.opportunity.vo.StageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 商机管理。列表 / 看板已接入行级数据权限拦截器（owner_id + dept_id）。
 */
@Tag(name = "商机管理")
@RestController
@RequestMapping("/opportunities")
@RequiredArgsConstructor
public class OpportunityController {

    private final OpportunityServiceImpl opportunityService;

    @Operation(summary = "创建商机")
    @PostMapping
    @PreAuthorize("hasAuthority('crm:opportunity:create')")
    public Result<Map<String, Object>> create(@Valid @RequestBody OpportunityCreateForm form) {
        return Result.ok(Map.of("id", opportunityService.createFromForm(form)));
    }

    @Operation(summary = "更新商机（阶段变更走 /{id}/stage）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:opportunity:update')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody OpportunityUpdateForm form) {
        opportunityService.updateFromForm(id, form);
        return Result.ok();
    }

    @Operation(summary = "商机详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:opportunity:list')")
    public Result<OpportunityVO> detail(@PathVariable Long id) {
        return Result.ok(opportunityService.detail(id));
    }

    @Operation(summary = "分页查询（customerId/stageCode/ownerId/时间范围）")
    @GetMapping
    @PreAuthorize("hasAuthority('crm:opportunity:list')")
    public Result<PageResult<OpportunityVO>> page(OpportunityQuery query) {
        return Result.ok(opportunityService.pageQuery(query));
    }

    @Operation(summary = "看板数据（一次查询返回全部阶段分组）")
    @GetMapping("/board")
    @PreAuthorize("hasAuthority('crm:opportunity:list')")
    public Result<List<BoardStage>> board() {
        return Result.ok(opportunityService.board());
    }

    @Operation(summary = "变更阶段（赢单回写成交信息+发布事件；丢单必填原因）")
    @PutMapping("/{id}/stage")
    @PreAuthorize("hasAuthority('crm:opportunity:stage')")
    public Result<Void> changeStage(@PathVariable Long id, @Valid @RequestBody StageChangeForm form) {
        opportunityService.changeStage(id, form);
        return Result.ok();
    }

    @Operation(summary = "销售预测（按阶段聚合，排除终态；一条聚合 SQL）")
    @GetMapping("/forecast")
    @PreAuthorize("hasAuthority('crm:opportunity:forecast')")
    public Result<ForecastVO> forecast(ForecastQuery query) {
        return Result.ok(opportunityService.forecast(query));
    }

    @Operation(summary = "阶段配置列表")
    @GetMapping("/stages")
    @PreAuthorize("hasAuthority('crm:opportunity:list')")
    public Result<List<StageVO>> stages() {
        return Result.ok(opportunityService.listStages());
    }

    @Operation(summary = "新增阶段配置")
    @PostMapping("/stages")
    @PreAuthorize("hasAuthority('crm:opportunity:stage-manage')")
    public Result<Map<String, Object>> createStage(@Valid @RequestBody StageForm form) {
        return Result.ok(Map.of("id", opportunityService.createStage(form)));
    }

    @Operation(summary = "修改阶段配置（停用需该阶段无商机）")
    @PutMapping("/stages/{id}")
    @PreAuthorize("hasAuthority('crm:opportunity:stage-manage')")
    public Result<Void> updateStage(@PathVariable Long id, @Valid @RequestBody StageForm form) {
        opportunityService.updateStage(id, form);
        return Result.ok();
    }
}
