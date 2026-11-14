package cn.easykits.crm.modules.product.controller;

import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.Result;
import cn.easykits.crm.modules.product.dto.OpportunityItemForm;
import cn.easykits.crm.modules.product.service.OpportunityItemServiceImpl;
import cn.easykits.crm.modules.product.vo.OpportunityItemVO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 商机产品明细管理。
 */
@Tag(name = "商机产品明细")
@RestController
@RequestMapping("/opportunity-items")
@RequiredArgsConstructor
public class OpportunityItemController {

    private final OpportunityItemServiceImpl opportunityItemService;

    @Operation(summary = "新增商机产品明细（小计服务端计算）")
    @PostMapping
    @PreAuthorize("hasAuthority('crm:opp-item:create')")
    public Result<Map<String, Object>> create(@Valid @RequestBody OpportunityItemForm form) {
        return Result.ok(Map.of("id", opportunityItemService.createFromForm(form)));
    }

    @Operation(summary = "修改商机产品明细")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:opp-item:update')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody OpportunityItemForm form) {
        opportunityItemService.updateFromForm(id, form);
        return Result.ok();
    }

    @Operation(summary = "删除商机产品明细（逻辑删除）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:opp-item:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        opportunityItemService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "商机产品明细详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:opp-item:list')")
    public Result<OpportunityItemVO> detail(@PathVariable Long id) {
        return Result.ok(opportunityItemService.detail(id));
    }

    @Operation(summary = "商机产品明细分页（按商机过滤）")
    @GetMapping
    @PreAuthorize("hasAuthority('crm:opp-item:list')")
    public Result<PageResult<OpportunityItemVO>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long opportunityId) {
        return Result.ok(opportunityItemService.pageQuery(pageNum, pageSize, opportunityId));
    }

    @Operation(summary = "某商机产品明细汇总金额")
    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('crm:opp-item:list')")
    public Result<BigDecimal> summary(@RequestParam Long opportunityId) {
        return Result.ok(opportunityItemService.summary(opportunityId));
    }
}
