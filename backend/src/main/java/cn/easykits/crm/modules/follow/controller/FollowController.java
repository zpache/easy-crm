package cn.easykits.crm.modules.follow.controller;

import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.Result;
import cn.easykits.crm.modules.follow.dto.FollowRecordCreateForm;
import cn.easykits.crm.modules.follow.dto.FollowRecordQuery;
import cn.easykits.crm.modules.follow.dto.FollowRecordUpdateForm;
import cn.easykits.crm.modules.follow.service.FollowRecordServiceImpl;
import cn.easykits.crm.modules.follow.vo.FollowRecordVO;
import cn.easykits.crm.modules.follow.vo.TimelineItemVO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 跟进记录管理 + 时间线聚合。
 */
@Tag(name = "跟进记录")
@RestController
@RequiredArgsConstructor
public class FollowController {

    private final FollowRecordServiceImpl followRecordService;

    @Operation(summary = "创建跟进记录（可选下次跟进时间→自动生成待办）")
    @PostMapping("/follow-records")
    @PreAuthorize("hasAuthority('crm:follow:create')")
    public Result<FollowRecordVO> create(@Valid @RequestBody FollowRecordCreateForm form) {
        return Result.ok(followRecordService.create(form));
    }

    @Operation(summary = "更新跟进记录")
    @PutMapping("/follow-records/{id}")
    @PreAuthorize("hasAuthority('crm:follow:update')")
    public Result<FollowRecordVO> update(@PathVariable Long id,
                                         @Valid @RequestBody FollowRecordUpdateForm form) {
        return Result.ok(followRecordService.update(id, form));
    }

    @Operation(summary = "删除跟进记录")
    @DeleteMapping("/follow-records/{id}")
    @PreAuthorize("hasAuthority('crm:follow:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        followRecordService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "跟进记录分页（bizType/bizId/ownerId/时间范围）")
    @GetMapping("/follow-records")
    @PreAuthorize("hasAuthority('crm:follow:list')")
    public Result<PageResult<FollowRecordVO>> page(FollowRecordQuery query) {
        return Result.ok(followRecordService.pageQuery(query));
    }

    @Operation(summary = "时间线聚合（跟进记录 + 待办 + 商机阶段变更，一条 UNION ALL 分页）")
    @GetMapping("/timeline")
    @PreAuthorize("hasAuthority('crm:follow:list')")
    public Result<PageResult<TimelineItemVO>> timeline(
            @RequestParam String bizType,
            @RequestParam Long bizId,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "20") long pageSize) {
        return Result.ok(followRecordService.timeline(bizType, bizId, pageNum, pageSize));
    }
}
