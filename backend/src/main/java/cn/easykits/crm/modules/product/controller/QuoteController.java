package cn.easykits.crm.modules.product.controller;

import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.Result;
import cn.easykits.crm.modules.product.dto.QuoteForm;
import cn.easykits.crm.modules.product.dto.QuoteQuery;
import cn.easykits.crm.modules.product.service.QuoteServiceImpl;
import cn.easykits.crm.modules.product.vo.QuoteVO;
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
 * 报价单管理（主表金额与明细汇总强一致）。
 */
@Tag(name = "报价单管理")
@RestController
@RequestMapping("/quotes")
@RequiredArgsConstructor
public class QuoteController {

    private final QuoteServiceImpl quoteService;

    @Operation(summary = "创建报价单（明细汇总必须等于总金额）")
    @PostMapping
    @PreAuthorize("hasAuthority('crm:quote:create')")
    public Result<Map<String, Object>> create(@Valid @RequestBody QuoteForm form) {
        return Result.ok(Map.of("id", quoteService.createFromForm(form)));
    }

    @Operation(summary = "更新报价单（明细汇总必须等于总金额）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:quote:update')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody QuoteForm form) {
        quoteService.updateFromForm(id, form);
        return Result.ok();
    }

    @Operation(summary = "删除报价单（连带逻辑删除明细）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:quote:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        quoteService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "报价单详情（含明细）")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:quote:list')")
    public Result<QuoteVO> detail(@PathVariable Long id) {
        return Result.ok(quoteService.detail(id));
    }

    @Operation(summary = "报价单分页")
    @GetMapping
    @PreAuthorize("hasAuthority('crm:quote:list')")
    public Result<PageResult<QuoteVO>> page(QuoteQuery query) {
        return Result.ok(quoteService.pageQuery(query));
    }
}
