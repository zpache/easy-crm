package cn.easykits.crm.modules.approval.controller;

import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.Result;
import cn.easykits.crm.modules.approval.dto.ApprovalQuery;
import cn.easykits.crm.modules.approval.service.ApprovalInstanceServiceImpl;
import cn.easykits.crm.modules.approval.vo.ApprovalInstanceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 审批中心入口（只读）。
 * <p>审批流对接外部审批系统，CRM 侧不提供通过 / 驳回动作，仅提供待办 / 已办 / 已发起列表与跳转。
 */
@Tag(name = "审批中心")
@RestController
@RequestMapping("/approvals")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalInstanceServiceImpl approvalService;

    @Operation(summary = "审批列表（box=todo 待办 / done 已办 / applied 已发起）")
    @GetMapping
    @PreAuthorize("hasAuthority('crm:approval:list')")
    public Result<PageResult<ApprovalInstanceVO>> page(ApprovalQuery query) {
        return Result.ok(approvalService.pageQuery(query));
    }

    @Operation(summary = "三个入口的数量（页签角标）")
    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('crm:approval:list')")
    public Result<Map<String, Long>> summary() {
        return Result.ok(approvalService.summary());
    }

    @Operation(summary = "审批实例详情（只读镜像）")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:approval:list')")
    public Result<ApprovalInstanceVO> detail(@PathVariable Long id) {
        return Result.ok(approvalService.detail(id));
    }
}
