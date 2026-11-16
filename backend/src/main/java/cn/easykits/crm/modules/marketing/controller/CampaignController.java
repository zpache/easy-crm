package cn.easykits.crm.modules.marketing.controller;

import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.Result;
import cn.easykits.crm.modules.marketing.dto.CampaignForm;
import cn.easykits.crm.modules.marketing.dto.CampaignQuery;
import cn.easykits.crm.modules.marketing.service.CampaignServiceImpl;
import cn.easykits.crm.modules.marketing.vo.CampaignVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * 营销活动管理。
 */
@Tag(name = "营销活动")
@RestController
@RequestMapping("/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignServiceImpl campaignService;

    @Operation(summary = "新增营销活动")
    @PostMapping
    @PreAuthorize("hasAuthority('crm:campaign:create')")
    public Result<Map<String, Object>> create(@Valid @RequestBody CampaignForm form) {
        return Result.ok(Map.of("id", campaignService.createFromForm(form)));
    }

    @Operation(summary = "修改营销活动")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:campaign:update')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CampaignForm form) {
        campaignService.updateFromForm(id, form);
        return Result.ok();
    }

    @Operation(summary = "删除营销活动（逻辑删除，线索上的活动归因保留）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:campaign:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        campaignService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "营销活动详情（含活动效果指标）")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:campaign:list')")
    public Result<CampaignVO> detail(@PathVariable Long id) {
        return Result.ok(campaignService.detail(id));
    }

    @Operation(summary = "营销活动分页（含线索数 / 转化率 / 赢单金额 / ROI）")
    @GetMapping
    @PreAuthorize("hasAuthority('crm:campaign:list')")
    public Result<PageResult<CampaignVO>> page(CampaignQuery query) {
        return Result.ok(campaignService.pageQuery(query));
    }

    @Operation(summary = "营销活动下拉选项（线索表单选择来源活动用，登录即可读）")
    @GetMapping("/options")
    public Result<List<Map<String, Object>>> options() {
        return Result.ok(campaignService.options());
    }
}
