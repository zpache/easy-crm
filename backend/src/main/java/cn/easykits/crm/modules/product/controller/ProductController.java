package cn.easykits.crm.modules.product.controller;

import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.Result;
import cn.easykits.crm.modules.product.dto.ProductForm;
import cn.easykits.crm.modules.product.dto.ProductQuery;
import cn.easykits.crm.modules.product.service.ProductServiceImpl;
import cn.easykits.crm.modules.product.vo.ProductVO;
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
 * 产品管理（产品域）。
 */
@Tag(name = "产品管理")
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductServiceImpl productService;

    @Operation(summary = "创建产品（编码唯一）")
    @PostMapping
    @PreAuthorize("hasAuthority('crm:product:create')")
    public Result<Map<String, Object>> create(@Valid @RequestBody ProductForm form) {
        return Result.ok(Map.of("id", productService.createFromForm(form)));
    }

    @Operation(summary = "更新产品")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:product:update')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ProductForm form) {
        productService.updateFromForm(id, form);
        return Result.ok();
    }

    @Operation(summary = "删除产品（逻辑删除）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:product:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "产品详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:product:list')")
    public Result<ProductVO> detail(@PathVariable Long id) {
        return Result.ok(productService.detail(id));
    }

    @Operation(summary = "产品分页（名称/编码/分类/状态）")
    @GetMapping
    @PreAuthorize("hasAuthority('crm:product:list')")
    public Result<PageResult<ProductVO>> page(ProductQuery query) {
        return Result.ok(productService.pageQuery(query));
    }
}
