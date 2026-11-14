package cn.easykits.crm.modules.payment.controller;

import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.Result;
import cn.easykits.crm.modules.payment.dto.PaymentPlanForm;
import cn.easykits.crm.modules.payment.service.PaymentPlanServiceImpl;
import cn.easykits.crm.modules.payment.vo.PaymentPlanVO;
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

import java.util.Map;

/**
 * 回款计划管理。
 */
@Tag(name = "回款计划")
@RestController
@RequestMapping("/payment-plans")
@RequiredArgsConstructor
public class PaymentPlanController {

    private final PaymentPlanServiceImpl paymentPlanService;

    @Operation(summary = "新增回款计划（同订单期次唯一）")
    @PostMapping
    @PreAuthorize("hasAuthority('crm:payment-plan:create')")
    public Result<Map<String, Object>> create(@Valid @RequestBody PaymentPlanForm form) {
        return Result.ok(Map.of("id", paymentPlanService.createFromForm(form)));
    }

    @Operation(summary = "修改回款计划（已核销计划不可改）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:payment-plan:update')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody PaymentPlanForm form) {
        paymentPlanService.updateFromForm(id, form);
        return Result.ok();
    }

    @Operation(summary = "删除回款计划（逻辑删除；已核销不可删）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:payment-plan:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        paymentPlanService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "回款计划详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:payment-plan:list')")
    public Result<PaymentPlanVO> detail(@PathVariable Long id) {
        return Result.ok(paymentPlanService.detail(id));
    }

    @Operation(summary = "回款计划分页（按订单/状态过滤）")
    @GetMapping
    @PreAuthorize("hasAuthority('crm:payment-plan:list')")
    public Result<PageResult<PaymentPlanVO>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Integer status) {
        return Result.ok(paymentPlanService.pageQuery(pageNum, pageSize, orderId, status));
    }
}
