package cn.easykits.crm.modules.support.controller;

import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.Result;
import cn.easykits.crm.modules.support.dto.SlaPolicyForm;
import cn.easykits.crm.modules.support.dto.SlaPolicyQuery;
import cn.easykits.crm.modules.support.service.SlaPolicyServiceImpl;
import cn.easykits.crm.modules.support.vo.SlaPolicyVO;
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
 * SLA 策略配置：工单类型 + 优先级 → 响应/解决小时数，可配置启停。
 */
@Tag(name = "SLA 策略")
@RestController
@RequestMapping("/sla-policies")
@RequiredArgsConstructor
public class SlaPolicyController {

    private final SlaPolicyServiceImpl slaPolicyService;

    @Operation(summary = "SLA 策略分页（类型/优先级/启停）")
    @GetMapping
    @PreAuthorize("hasAuthority('crm:sla:list')")
    public Result<PageResult<SlaPolicyVO>> page(SlaPolicyQuery query) {
        return Result.ok(slaPolicyService.pageQuery(query));
    }

    @Operation(summary = "SLA 策略详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:sla:list')")
    public Result<SlaPolicyVO> detail(@PathVariable Long id) {
        return Result.ok(slaPolicyService.detail(id));
    }

    @Operation(summary = "新增 SLA 策略（类型+优先级唯一）")
    @PostMapping
    @PreAuthorize("hasAuthority('crm:sla:create')")
    public Result<Map<String, Object>> create(@Valid @RequestBody SlaPolicyForm form) {
        return Result.ok(Map.of("id", slaPolicyService.create(form)));
    }

    @Operation(summary = "修改 SLA 策略（含启停）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:sla:update')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SlaPolicyForm form) {
        slaPolicyService.update(id, form);
        return Result.ok();
    }

    @Operation(summary = "删除 SLA 策略（逻辑删除）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:sla:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        slaPolicyService.delete(id);
        return Result.ok();
    }
}
