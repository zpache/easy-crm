package cn.easykits.crm.modules.report.controller;

import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.Result;
import cn.easykits.crm.modules.report.dto.SalesTargetForm;
import cn.easykits.crm.modules.report.dto.SalesTargetQuery;
import cn.easykits.crm.modules.report.service.SalesTargetServiceImpl;
import cn.easykits.crm.modules.report.vo.SalesTargetVO;
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

import java.util.Map;

/**
 * 销售目标管理（报表中心）。
 */
@Tag(name = "销售目标")
@RestController
@RequestMapping("/sales-targets")
@RequiredArgsConstructor
public class SalesTargetController {

    private final SalesTargetServiceImpl salesTargetService;

    @Operation(summary = "新增销售目标（同年月+部门+负责人唯一）")
    @PostMapping
    @PreAuthorize("hasAuthority('crm:sales-target:create')")
    public Result<Map<String, Object>> create(@Valid @RequestBody SalesTargetForm form) {
        return Result.ok(Map.of("id", salesTargetService.createFromForm(form)));
    }

    @Operation(summary = "修改销售目标")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:sales-target:update')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SalesTargetForm form) {
        salesTargetService.updateFromForm(id, form);
        return Result.ok();
    }

    @Operation(summary = "删除销售目标（逻辑删除）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:sales-target:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        salesTargetService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "销售目标详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:sales-target:list')")
    public Result<SalesTargetVO> detail(@PathVariable Long id) {
        return Result.ok(salesTargetService.detail(id));
    }

    @Operation(summary = "销售目标分页")
    @GetMapping
    @PreAuthorize("hasAuthority('crm:sales-target:list')")
    public Result<PageResult<SalesTargetVO>> page(SalesTargetQuery query) {
        return Result.ok(salesTargetService.pageQuery(query));
    }
}
