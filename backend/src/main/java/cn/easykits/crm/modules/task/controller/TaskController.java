package cn.easykits.crm.modules.task.controller;

import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.Result;
import cn.easykits.crm.modules.task.dto.TaskCreateForm;
import cn.easykits.crm.modules.task.dto.TaskQuery;
import cn.easykits.crm.modules.task.dto.TaskUpdateForm;
import cn.easykits.crm.modules.task.service.TaskServiceImpl;
import cn.easykits.crm.modules.task.vo.TaskVO;
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

import java.util.Map;

/**
 * 待办任务管理。
 */
@Tag(name = "待办任务")
@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskServiceImpl taskService;

    @Operation(summary = "创建任务（手动）")
    @PostMapping
    @PreAuthorize("hasAuthority('crm:task:create')")
    public Result<Map<String, Object>> create(@Valid @RequestBody TaskCreateForm form) {
        return Result.ok(Map.of("id", taskService.createFromForm(form)));
    }

    @Operation(summary = "更新任务（已完成不可编辑）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:task:update')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody TaskUpdateForm form) {
        taskService.updateFromForm(id, form);
        return Result.ok();
    }

    @Operation(summary = "完成任务")
    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('crm:task:complete')")
    public Result<Void> complete(@PathVariable Long id) {
        taskService.complete(id);
        return Result.ok();
    }

    @Operation(summary = "任务分页（status/ownerId/dueTime 范围；status=2 筛已过期）")
    @GetMapping
    @PreAuthorize("hasAuthority('crm:task:list')")
    public Result<PageResult<TaskVO>> page(TaskQuery query) {
        return Result.ok(taskService.pageQuery(query));
    }

    @Operation(summary = "我的待办（未完成且临近 3 天内或已过期）")
    @GetMapping("/todo")
    @PreAuthorize("hasAuthority('crm:task:list')")
    public Result<PageResult<TaskVO>> todo(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(taskService.todo(pageNum, pageSize));
    }
}
