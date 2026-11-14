package cn.easykits.crm.modules.contract.controller;

import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.Result;
import cn.easykits.crm.modules.contract.dto.ContractForm;
import cn.easykits.crm.modules.contract.dto.ContractQuery;
import cn.easykits.crm.modules.contract.dto.ContractStatusForm;
import cn.easykits.crm.modules.contract.service.ContractServiceImpl;
import cn.easykits.crm.modules.contract.vo.ContractVO;
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
 * 合同管理（交易域）。
 */
@Tag(name = "合同管理")
@RestController
@RequestMapping("/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractServiceImpl contractService;

    @Operation(summary = "创建合同（编号唯一）")
    @PostMapping
    @PreAuthorize("hasAuthority('crm:contract:create')")
    public Result<Map<String, Object>> create(@Valid @RequestBody ContractForm form) {
        return Result.ok(Map.of("id", contractService.createFromForm(form)));
    }

    @Operation(summary = "更新合同")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:contract:update')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ContractForm form) {
        contractService.updateFromForm(id, form);
        return Result.ok();
    }

    @Operation(summary = "合同状态流转")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('crm:contract:update')")
    public Result<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody ContractStatusForm form) {
        contractService.changeStatus(id, form.getStatus());
        return Result.ok();
    }

    @Operation(summary = "删除合同（逻辑删除）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:contract:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        contractService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "合同详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:contract:list')")
    public Result<ContractVO> detail(@PathVariable Long id) {
        return Result.ok(contractService.detail(id));
    }

    @Operation(summary = "合同分页")
    @GetMapping
    @PreAuthorize("hasAuthority('crm:contract:list')")
    public Result<PageResult<ContractVO>> page(ContractQuery query) {
        return Result.ok(contractService.pageQuery(query));
    }
}
