package cn.easykits.crm.modules.customer.controller;

import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.Result;
import cn.easykits.crm.modules.customer.dto.CustomerCreateForm;
import cn.easykits.crm.modules.customer.dto.CustomerQuery;
import cn.easykits.crm.modules.customer.dto.MergeForm;
import cn.easykits.crm.modules.customer.service.CustomerServiceImpl;
import cn.easykits.crm.modules.customer.vo.Customer360VO;
import cn.easykits.crm.modules.customer.vo.CustomerVO;
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

import java.util.Map;

/**
 * 客户管理。列表查询已接入行级数据权限拦截器（owner_id + dept_id）。
 */
@Tag(name = "客户管理")
@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerServiceImpl customerService;

    @Operation(summary = "创建客户（名称+信用代码组合查重，重复阻断）")
    @PostMapping
    @PreAuthorize("hasAuthority('crm:customer:create')")
    public Result<Map<String, Object>> create(@Valid @RequestBody CustomerCreateForm form) {
        Long id = customerService.createFromForm(form);
        return Result.ok(Map.of("id", id));
    }

    @Operation(summary = "更新客户")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:customer:update')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CustomerCreateForm form) {
        customerService.updateFromForm(id, form);
        return Result.ok();
    }

    @Operation(summary = "客户详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:customer:list')")
    public Result<CustomerVO> detail(@PathVariable Long id) {
        return Result.ok(customerService.detail(id));
    }

    @Operation(summary = "客户 360 视图（基本信息 + 联系人；商机/跟进待后续轮次填充）")
    @GetMapping("/{id}/360")
    @PreAuthorize("hasAuthority('crm:customer:list')")
    public Result<Customer360VO> view360(@PathVariable Long id) {
        return Result.ok(customerService.view360(id));
    }

    @Operation(summary = "分页查询（name/industry/status/ownerId 过滤；默认排除已合并）")
    @GetMapping
    @PreAuthorize("hasAuthority('crm:customer:list')")
    public Result<PageResult<CustomerVO>> page(CustomerQuery query) {
        return Result.ok(customerService.pageQuery(query));
    }

    @Operation(summary = "客户合并：source 合并到 target（事务迁移联系人/线索关联）")
    @PostMapping("/merge")
    @PreAuthorize("hasAuthority('crm:customer:merge')")
    public Result<Void> merge(@Valid @RequestBody MergeForm form) {
        customerService.merge(form.getSourceId(), form.getTargetId());
        return Result.ok();
    }

    @Operation(summary = "放入公海")
    @PostMapping("/{id}/to-pool")
    @PreAuthorize("hasAuthority('crm:customer:to-pool')")
    public Result<Void> toPool(@PathVariable Long id) {
        customerService.toPool(id);
        return Result.ok();
    }

    @Operation(summary = "从公海领取")
    @PostMapping("/{id}/claim")
    @PreAuthorize("hasAuthority('crm:customer:claim')")
    public Result<Void> claim(@PathVariable Long id) {
        customerService.claim(id);
        return Result.ok();
    }
}
