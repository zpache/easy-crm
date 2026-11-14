package cn.easykits.crm.modules.support.controller;

import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.Result;
import cn.easykits.crm.modules.support.dto.SatisfactionForm;
import cn.easykits.crm.modules.support.dto.SatisfactionQuery;
import cn.easykits.crm.modules.support.service.SatisfactionServiceImpl;
import cn.easykits.crm.modules.support.vo.SatisfactionStatsVO;
import cn.easykits.crm.modules.support.vo.SatisfactionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 满意度评价：工单完成时评价（一单一评）、分页列表、评分分布统计。
 */
@Tag(name = "满意度评价")
@RestController
@RequestMapping("/satisfactions")
@RequiredArgsConstructor
public class SatisfactionController {

    private final SatisfactionServiceImpl satisfactionService;

    @Operation(summary = "满意度分页列表（工单/评分/评价时间范围）")
    @GetMapping
    @PreAuthorize("hasAuthority('crm:satisfaction:list')")
    public Result<PageResult<SatisfactionVO>> page(SatisfactionQuery query) {
        return Result.ok(satisfactionService.pageQuery(query));
    }

    @Operation(summary = "满意度统计（总数/平均分/1-5 分分布）")
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('crm:satisfaction:list')")
    public Result<SatisfactionStatsVO> stats(SatisfactionQuery query) {
        return Result.ok(satisfactionService.stats(query));
    }

    @Operation(summary = "评价工单（仅已完成工单；一单一评）")
    @PostMapping
    @PreAuthorize("hasAuthority('crm:satisfaction:create')")
    public Result<Map<String, Object>> create(@Valid @RequestBody SatisfactionForm form) {
        return Result.ok(Map.of("id", satisfactionService.create(form)));
    }
}
