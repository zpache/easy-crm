package cn.easykits.crm.modules.support.controller;

import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.Result;
import cn.easykits.crm.modules.support.dto.KnowledgeForm;
import cn.easykits.crm.modules.support.dto.KnowledgeQuery;
import cn.easykits.crm.modules.support.service.KnowledgeServiceImpl;
import cn.easykits.crm.modules.support.vo.KnowledgeVO;
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
 * 知识库：CRUD + 检索 + 浏览量递增。
 */
@Tag(name = "知识库")
@RestController
@RequestMapping("/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeServiceImpl knowledgeService;

    @Operation(summary = "知识库分页检索（关键字/分类/发布状态）")
    @GetMapping
    @PreAuthorize("hasAuthority('crm:knowledge:list')")
    public Result<PageResult<KnowledgeVO>> page(KnowledgeQuery query) {
        return Result.ok(knowledgeService.pageQuery(query));
    }

    @Operation(summary = "知识条目详情（纯读）")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:knowledge:list')")
    public Result<KnowledgeVO> detail(@PathVariable Long id) {
        return Result.ok(knowledgeService.detail(id));
    }

    @Operation(summary = "浏览量 +1")
    @PostMapping("/{id}/view")
    @PreAuthorize("hasAuthority('crm:knowledge:list')")
    public Result<Void> view(@PathVariable Long id) {
        knowledgeService.view(id);
        return Result.ok();
    }

    @Operation(summary = "新增知识条目")
    @PostMapping
    @PreAuthorize("hasAuthority('crm:knowledge:create')")
    public Result<Map<String, Object>> create(@Valid @RequestBody KnowledgeForm form) {
        return Result.ok(Map.of("id", knowledgeService.create(form)));
    }

    @Operation(summary = "修改知识条目（含发布状态）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:knowledge:update')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody KnowledgeForm form) {
        knowledgeService.update(id, form);
        return Result.ok();
    }

    @Operation(summary = "删除知识条目（逻辑删除）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:knowledge:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        knowledgeService.delete(id);
        return Result.ok();
    }
}
