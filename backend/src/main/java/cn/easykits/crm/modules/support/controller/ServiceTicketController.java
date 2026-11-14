package cn.easykits.crm.modules.support.controller;

import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.Result;
import cn.easykits.crm.modules.support.dto.ServiceTicketCreateForm;
import cn.easykits.crm.modules.support.dto.ServiceTicketQuery;
import cn.easykits.crm.modules.support.dto.ServiceTicketUpdateForm;
import cn.easykits.crm.modules.support.dto.TicketAssignForm;
import cn.easykits.crm.modules.support.dto.TicketCloseForm;
import cn.easykits.crm.modules.support.dto.TicketStatusChangeForm;
import cn.easykits.crm.modules.support.service.ServiceTicketServiceImpl;
import cn.easykits.crm.modules.support.vo.ServiceTicketLogVO;
import cn.easykits.crm.modules.support.vo.ServiceTicketVO;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 服务工单管理：分页 / 详情 / 创建 / 编辑 / 状态流转 / 派单 / 关闭 / 流转日志。
 */
@Tag(name = "服务工单")
@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class ServiceTicketController {

    private final ServiceTicketServiceImpl serviceTicketService;

    @Operation(summary = "创建工单（按 SLA 策略计算应响应/应解决时间）")
    @PostMapping
    @PreAuthorize("hasAuthority('crm:ticket:create')")
    public Result<Map<String, Object>> create(@Valid @RequestBody ServiceTicketCreateForm form) {
        return Result.ok(Map.of("id", serviceTicketService.create(form)));
    }

    @Operation(summary = "编辑工单（终态不可编辑）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:ticket:update')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ServiceTicketUpdateForm form) {
        serviceTicketService.update(id, form);
        return Result.ok();
    }

    @Operation(summary = "工单分页（关键字/状态/类型/优先级/客户/受理人/超时/时间范围）")
    @GetMapping
    @PreAuthorize("hasAuthority('crm:ticket:list')")
    public Result<PageResult<ServiceTicketVO>> page(ServiceTicketQuery query) {
        return Result.ok(serviceTicketService.pageQuery(query));
    }

    @Operation(summary = "工单详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:ticket:list')")
    public Result<ServiceTicketVO> detail(@PathVariable Long id) {
        return Result.ok(serviceTicketService.detail(id));
    }

    @Operation(summary = "工单流转日志（时间线）")
    @GetMapping("/{id}/logs")
    @PreAuthorize("hasAuthority('crm:ticket:list')")
    public Result<List<ServiceTicketLogVO>> logs(@PathVariable Long id) {
        return Result.ok(serviceTicketService.logs(id));
    }

    @Operation(summary = "工单状态流转（终态不可再变，校验合法转移）")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('crm:ticket:status')")
    public Result<Void> changeStatus(@PathVariable Long id,
                                     @Valid @RequestBody TicketStatusChangeForm form) {
        serviceTicketService.changeStatus(id, form);
        return Result.ok();
    }

    @Operation(summary = "派单（指定受理人；待受理→已派单）")
    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAuthority('crm:ticket:assign')")
    public Result<Void> assign(@PathVariable Long id, @Valid @RequestBody TicketAssignForm form) {
        serviceTicketService.assign(id, form);
        return Result.ok();
    }

    @Operation(summary = "关闭工单（任意非终态→已关闭）")
    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('crm:ticket:close')")
    public Result<Void> close(@PathVariable Long id, @Valid @RequestBody TicketCloseForm form) {
        serviceTicketService.close(id, form);
        return Result.ok();
    }
}
