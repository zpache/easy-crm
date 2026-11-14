package cn.easykits.crm.modules.payment.controller;

import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.Result;
import cn.easykits.crm.modules.payment.dto.InvoiceForm;
import cn.easykits.crm.modules.payment.service.InvoiceServiceImpl;
import cn.easykits.crm.modules.payment.vo.InvoiceVO;
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
 * 开票申请管理。
 */
@Tag(name = "开票申请")
@RestController
@RequestMapping("/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceServiceImpl invoiceService;

    @Operation(summary = "新增开票申请")
    @PostMapping
    @PreAuthorize("hasAuthority('crm:invoice:create')")
    public Result<Map<String, Object>> create(@Valid @RequestBody InvoiceForm form) {
        return Result.ok(Map.of("id", invoiceService.createFromForm(form)));
    }

    @Operation(summary = "修改开票申请（已开票/已作废不可改）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:invoice:update')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody InvoiceForm form) {
        invoiceService.updateFromForm(id, form);
        return Result.ok();
    }

    @Operation(summary = "开票状态流转（0 待开票 / 1 已开票 / 2 已作废）")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('crm:invoice:update')")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        invoiceService.changeStatus(id, status);
        return Result.ok();
    }

    @Operation(summary = "删除开票申请（逻辑删除）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:invoice:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        invoiceService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "开票申请详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:invoice:list')")
    public Result<InvoiceVO> detail(@PathVariable Long id) {
        return Result.ok(invoiceService.detail(id));
    }

    @Operation(summary = "开票申请分页")
    @GetMapping
    @PreAuthorize("hasAuthority('crm:invoice:list')")
    public Result<PageResult<InvoiceVO>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Integer status) {
        return Result.ok(invoiceService.pageQuery(pageNum, pageSize, orderId, customerId, status));
    }
}
