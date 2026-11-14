package cn.easykits.crm.modules.product.controller;

import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.Result;
import cn.easykits.crm.modules.product.dto.PriceListForm;
import cn.easykits.crm.modules.product.service.PriceListServiceImpl;
import cn.easykits.crm.modules.product.vo.PriceListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 产品价目表管理。
 */
@Tag(name = "产品价目表")
@RestController
@RequestMapping("/price-lists")
@RequiredArgsConstructor
public class PriceListController {

    private final PriceListServiceImpl priceListService;

    @Operation(summary = "新增价格")
    @PostMapping
    @PreAuthorize("hasAuthority('crm:price:create')")
    public Result<Map<String, Object>> create(@Valid @RequestBody PriceListForm form) {
        return Result.ok(Map.of("id", priceListService.createFromForm(form)));
    }

    @Operation(summary = "修改价格")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:price:update')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody PriceListForm form) {
        priceListService.updateFromForm(id, form);
        return Result.ok();
    }

    @Operation(summary = "删除价格（逻辑删除）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:price:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        priceListService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "价格详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:price:list')")
    public Result<PriceListVO> detail(@PathVariable Long id) {
        return Result.ok(priceListService.detail(id));
    }

    @Operation(summary = "价格分页（按产品/状态过滤）")
    @GetMapping
    @PreAuthorize("hasAuthority('crm:price:list')")
    public Result<PageResult<PriceListVO>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Integer status) {
        return Result.ok(priceListService.pageQuery(pageNum, pageSize, productId, status));
    }

    @Operation(summary = "查询某产品在指定日期生效的价格")
    @GetMapping("/effective")
    @PreAuthorize("hasAuthority('crm:price:list')")
    public Result<List<PriceListVO>> effective(
            @RequestParam Long productId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.ok(priceListService.effectivePrices(productId, date));
    }
}
