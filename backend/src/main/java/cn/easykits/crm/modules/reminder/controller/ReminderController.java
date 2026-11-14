package cn.easykits.crm.modules.reminder.controller;

import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.Result;
import cn.easykits.crm.modules.reminder.dto.ReminderQuery;
import cn.easykits.crm.modules.reminder.service.ReminderService;
import cn.easykits.crm.modules.reminder.vo.ReminderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 消息提醒读取链路：列表 / 未读数 / 标记已读 / 全部已读。
 * <p>所有数据按当前登录用户 ownerId 过滤（见 ReminderService）。
 * 写操作（PUT）由 OperLogAspect 自动记录操作日志。
 */
@Tag(name = "消息提醒")
@RestController
@RequestMapping("/reminders")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderService reminderService;

    @Operation(summary = "提醒分页（按当前用户 ownerId 过滤；onlyUnread=true 只查未读）")
    @GetMapping
    @PreAuthorize("hasAuthority('crm:reminder:list')")
    public Result<PageResult<ReminderVO>> page(ReminderQuery query) {
        return Result.ok(reminderService.pageQuery(query));
    }

    @Operation(summary = "当前用户未读提醒条数（铃铛角标）")
    @GetMapping("/unread-count")
    @PreAuthorize("hasAuthority('crm:reminder:list')")
    public Result<Map<String, Object>> unreadCount() {
        return Result.ok(Map.of("count", reminderService.unreadCount()));
    }

    @Operation(summary = "标记单条提醒为已读")
    @PutMapping("/{id}/read")
    @PreAuthorize("hasAuthority('crm:reminder:read')")
    public Result<Void> markRead(@PathVariable Long id) {
        reminderService.markRead(id);
        return Result.ok();
    }

    @Operation(summary = "当前用户全部提醒标记已读")
    @PutMapping("/read-all")
    @PreAuthorize("hasAuthority('crm:reminder:read')")
    public Result<Integer> markAllRead() {
        return Result.ok(reminderService.markAllRead());
    }
}
