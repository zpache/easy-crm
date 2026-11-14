package cn.easykits.crm.modules.payment.controller;

import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.Result;
import cn.easykits.crm.modules.payment.dto.PaymentForm;
import cn.easykits.crm.modules.payment.dto.PaymentQuery;
import cn.easykits.crm.modules.payment.dto.PaymentWriteOffForm;
import cn.easykits.crm.modules.payment.service.PaymentServiceImpl;
import cn.easykits.crm.modules.payment.vo.PaymentVO;
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
 * 回款记录管理（含核销）。
 */
@Tag(name = "回款记录")
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentServiceImpl paymentService;

    @Operation(summary = "登记回款（默认未核销）")
    @PostMapping
    @PreAuthorize("hasAuthority('crm:payment:create')")
    public Result<Map<String, Object>> create(@Valid @RequestBody PaymentForm form) {
        return Result.ok(Map.of("id", paymentService.createFromForm(form)));
    }

    @Operation(summary = "修改回款记录（已核销不可改）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:payment:update')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody PaymentForm form) {
        paymentService.updateFromForm(id, form);
        return Result.ok();
    }

    @Operation(summary = "删除回款记录（已核销不可删）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:payment:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        paymentService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "回款核销（累加计划已核销金额并回写计划状态）")
    @PostMapping("/{id}/writeoff")
    @PreAuthorize("hasAuthority('crm:payment:writeoff')")
    public Result<Void> writeoff(@PathVariable Long id, @Valid @RequestBody PaymentWriteOffForm form) {
        paymentService.writeoff(id, form.getPlanId());
        return Result.ok();
    }

    @Operation(summary = "回款记录详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:payment:list')")
    public Result<PaymentVO> detail(@PathVariable Long id) {
        return Result.ok(paymentService.detail(id));
    }

    @Operation(summary = "回款记录分页")
    @GetMapping
    @PreAuthorize("hasAuthority('crm:payment:list')")
    public Result<PageResult<PaymentVO>> page(PaymentQuery query) {
        return Result.ok(paymentService.pageQuery(query));
    }
}
