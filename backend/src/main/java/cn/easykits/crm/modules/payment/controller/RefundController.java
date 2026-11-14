package cn.easykits.crm.modules.payment.controller;

import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.Result;
import cn.easykits.crm.modules.payment.dto.RefundForm;
import cn.easykits.crm.modules.payment.service.RefundServiceImpl;
import cn.easykits.crm.modules.payment.vo.RefundVO;
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
 * 退货 / 退款管理。
 */
@Tag(name = "退货退款")
@RestController
@RequestMapping("/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundServiceImpl refundService;

    @Operation(summary = "新增退款申请")
    @PostMapping
    @PreAuthorize("hasAuthority('crm:refund:create')")
    public Result<Map<String, Object>> create(@Valid @RequestBody RefundForm form) {
        return Result.ok(Map.of("id", refundService.createFromForm(form)));
    }

    @Operation(summary = "修改退款申请（已退款/已驳回不可改）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:refund:update')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody RefundForm form) {
        refundService.updateFromForm(id, form);
        return Result.ok();
    }

    @Operation(summary = "退款状态流转（0 申请中 / 1 已审核 / 2 已退款 / 3 已驳回）")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('crm:refund:update')")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        refundService.changeStatus(id, status);
        return Result.ok();
    }

    @Operation(summary = "删除退款申请（逻辑删除）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:refund:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        refundService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "退款申请详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:refund:list')")
    public Result<RefundVO> detail(@PathVariable Long id) {
        return Result.ok(refundService.detail(id));
    }

    @Operation(summary = "退款申请分页")
    @GetMapping
    @PreAuthorize("hasAuthority('crm:refund:list')")
    public Result<PageResult<RefundVO>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Integer status) {
        return Result.ok(refundService.pageQuery(pageNum, pageSize, orderId, customerId, status));
    }
}
