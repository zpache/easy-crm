package com.easykits.crm.modules.lead.controller;

import com.easykits.crm.common.api.PageResult;
import com.easykits.crm.common.api.Result;
import com.easykits.crm.modules.lead.dto.ConvertForm;
import com.easykits.crm.modules.lead.dto.LeadCreateForm;
import com.easykits.crm.modules.lead.dto.LeadQuery;
import com.easykits.crm.modules.lead.dto.LeadUpdateForm;
import com.easykits.crm.modules.lead.service.LeadService;
import com.easykits.crm.modules.lead.vo.LeadConvertVO;
import com.easykits.crm.modules.lead.vo.LeadImportResultVO;
import com.easykits.crm.modules.lead.vo.LeadVO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 线索管理。列表查询已接入行级数据权限拦截器
 * （crm_lead 的 owner_id + dept_id 条件按登录用户自动注入）。
 */
@Tag(name = "线索管理")
@RestController
@RequestMapping("/leads")
@RequiredArgsConstructor
public class LeadController {

    private final LeadService leadService;

    @Operation(summary = "创建线索")
    @PostMapping
    @PreAuthorize("hasAuthority('crm:lead:create')")
    public Result<LeadVO> create(@Valid @RequestBody LeadCreateForm form) {
        return Result.ok(leadService.create(form));
    }

    @Operation(summary = "更新线索（status 可选，按状态机校验）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:lead:update')")
    public Result<LeadVO> update(@PathVariable Long id, @Valid @RequestBody LeadUpdateForm form) {
        return Result.ok(leadService.update(id, form));
    }

    @Operation(summary = "线索详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:lead:list')")
    public Result<LeadVO> detail(@PathVariable Long id) {
        return Result.ok(leadService.detail(id));
    }

    @Operation(summary = "分页查询（name/phone/status/ownerId/source 过滤；ownerId=-1 查公海）")
    @GetMapping
    @PreAuthorize("hasAuthority('crm:lead:list')")
    public Result<PageResult<LeadVO>> page(LeadQuery query) {
        return Result.ok(leadService.pageQuery(query));
    }

    @Operation(summary = "线索转化：创建/关联客户与联系人（可选）+ 创建商机，返回商机 ID")
    @PostMapping("/{id}/convert")
    @PreAuthorize("hasAuthority('crm:lead:convert')")
    public Result<LeadConvertVO> convert(@PathVariable Long id, @Valid @RequestBody ConvertForm form) {
        return Result.ok(leadService.convert(id, form));
    }

    @Operation(summary = "放入公海（owner_id 置空，状态回退新建）")
    @PostMapping("/{id}/to-pool")
    @PreAuthorize("hasAuthority('crm:lead:to-pool')")
    public Result<Void> toPool(@PathVariable Long id) {
        leadService.toPool(id);
        return Result.ok();
    }

    @Operation(summary = "从公海领取（负责人置为当前用户）")
    @PostMapping("/{id}/claim")
    @PreAuthorize("hasAuthority('crm:lead:claim')")
    public Result<Void> claim(@PathVariable Long id) {
        leadService.claim(id);
        return Result.ok();
    }

    @Operation(summary = "Excel 批量导入（逐行校验，错误行集中返回）")
    @PostMapping("/import")
    @PreAuthorize("hasAuthority('crm:lead:import')")
    public Result<LeadImportResultVO> importExcel(@RequestParam("file") MultipartFile file) {
        return Result.ok(leadService.importExcel(file));
    }
}
