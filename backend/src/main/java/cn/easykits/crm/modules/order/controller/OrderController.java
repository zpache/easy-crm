package cn.easykits.crm.modules.order.controller;

import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.Result;
import cn.easykits.crm.modules.order.dto.OrderForm;
import cn.easykits.crm.modules.order.dto.OrderQuery;
import cn.easykits.crm.modules.order.dto.OrderStatusForm;
import cn.easykits.crm.modules.order.service.OrderServiceImpl;
import cn.easykits.crm.modules.order.vo.OrderItemVO;
import cn.easykits.crm.modules.order.vo.OrderVO;
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
 * 订单管理（交易域核心）：主表金额与明细汇总强一致、状态机流转。
 */
@Tag(name = "订单管理")
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderServiceImpl orderService;

    @Operation(summary = "创建订单（明细汇总必须等于总金额）")
    @PostMapping
    @PreAuthorize("hasAuthority('crm:order:create')")
    public Result<Map<String, Object>> create(@Valid @RequestBody OrderForm form) {
        return Result.ok(Map.of("id", orderService.createFromForm(form)));
    }

    @Operation(summary = "更新订单（明细汇总必须等于总金额；终态不可编辑）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:order:update')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody OrderForm form) {
        orderService.updateFromForm(id, form);
        return Result.ok();
    }

    @Operation(summary = "订单状态流转（草稿→待确认→已确认→已交付→已完成 / 已取消）")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('crm:order:status')")
    public Result<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody OrderStatusForm form) {
        orderService.changeStatus(id, form);
        return Result.ok();
    }

    @Operation(summary = "删除订单（连带逻辑删除明细；终态不可删）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:order:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        orderService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "订单详情（含明细）")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:order:list')")
    public Result<OrderVO> detail(@PathVariable Long id) {
        return Result.ok(orderService.detail(id));
    }

    @Operation(summary = "订单明细列表")
    @GetMapping("/{id}/items")
    @PreAuthorize("hasAuthority('crm:order:list')")
    public Result<List<OrderItemVO>> items(@PathVariable Long id) {
        return Result.ok(orderService.items(id));
    }

    @Operation(summary = "订单分页")
    @GetMapping
    @PreAuthorize("hasAuthority('crm:order:list')")
    public Result<PageResult<OrderVO>> page(OrderQuery query) {
        return Result.ok(orderService.pageQuery(query));
    }
}
