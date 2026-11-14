package cn.easykits.crm.modules.task.service;

import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.ResultCode;
import cn.easykits.crm.common.context.LoginUser;
import cn.easykits.crm.common.context.LoginUserContext;
import cn.easykits.crm.common.exception.BizException;
import cn.easykits.crm.entity.Task;
import cn.easykits.crm.mapper.SysUserMapper;
import cn.easykits.crm.mapper.TaskMapper;
import cn.easykits.crm.modules.follow.constant.FollowBizTypes;
import cn.easykits.crm.modules.task.dto.TaskCreateForm;
import cn.easykits.crm.modules.task.dto.TaskQuery;
import cn.easykits.crm.modules.task.dto.TaskUpdateForm;
import cn.easykits.crm.modules.task.vo.TaskVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 待办任务服务。状态模型：落库 0 待办 / 1 已完成；「已过期」= 未完成且 due_time < NOW，
 * 查询时动态计算（VO.status=2），不做落库——避免每分钟定时任务扫表，
 * 过期判定永远精确（决策说明见 DDL 注释与 README）。
 */
@Service
@RequiredArgsConstructor
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> {

    private final SysUserMapper sysUserMapper;

    /**
     * 手动创建任务：负责人为当前登录用户；关联业务类型校验。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createFromForm(TaskCreateForm form) {
        if (StringUtils.hasText(form.getBizType())) {
            if (!FollowBizTypes.isValid(form.getBizType())) {
                throw new BizException("业务类型不合法: " + form.getBizType());
            }
            if (form.getBizId() == null) {
                throw new BizException("填写业务类型时必须指定业务 ID");
            }
        }
        LoginUser user = LoginUserContext.require();
        Task task = new Task();
        task.setTitle(form.getTitle());
        task.setContent(form.getContent());
        task.setDueTime(form.getDueTime());
        task.setStatus(0);
        task.setBizType(StringUtils.hasText(form.getBizType()) ? form.getBizType() : null);
        task.setBizId(form.getBizId());
        task.setOwnerId(user.userId());
        task.setSource("MANUAL");
        save(task);
        return task.getId();
    }

    /**
     * 更新任务（已完成不可编辑）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateFromForm(Long id, TaskUpdateForm form) {
        Task task = getById(id);
        if (task == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "任务不存在");
        }
        if (task.getStatus() == 1) {
            throw new BizException("已完成的任务不可编辑");
        }
        if (StringUtils.hasText(form.getTitle())) {
            task.setTitle(form.getTitle());
        }
        task.setContent(form.getContent() != null ? form.getContent() : task.getContent());
        task.setDueTime(form.getDueTime() != null ? form.getDueTime() : task.getDueTime());
        if (!updateById(task)) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    /**
     * 完成任务：status → 1。已过期任务同样可被补完成。
     */
    @Transactional(rollbackFor = Exception.class)
    public void complete(Long id) {
        Task task = getById(id);
        if (task == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "任务不存在");
        }
        if (task.getStatus() == 1) {
            throw new BizException("任务已完成，请勿重复操作");
        }
        task.setStatus(1);
        if (!updateById(task)) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    /**
     * 分页查询（status/ownerId/dueTime 范围）；status=2 表示筛「已过期」
     * （动态条件：status=0 且 due_time < NOW）。
     */
    public PageResult<TaskVO> pageQuery(TaskQuery query) {
        boolean expiredFilter = query.getStatus() != null && query.getStatus() == 2;
        Page<Task> page = page(Page.of(query.getPageNum(), query.getPageSize()),
                Wrappers.<Task>lambdaQuery()
                        // 已过期筛选：未完成且超期
                        .eq(!expiredFilter && query.getStatus() != null, Task::getStatus, query.getStatus())
                        .eq(expiredFilter, Task::getStatus, 0)
                        .lt(expiredFilter, Task::getDueTime, LocalDateTime.now())
                        .isNull(query.getOwnerId() != null && query.getOwnerId() == -1, Task::getOwnerId)
                        .eq(query.getOwnerId() != null && query.getOwnerId() != -1,
                                Task::getOwnerId, query.getOwnerId())
                        .ge(query.getDueTimeStart() != null, Task::getDueTime, query.getDueTimeStart())
                        .le(query.getDueTimeEnd() != null, Task::getDueTime, query.getDueTimeEnd())
                        .orderByAsc(Task::getStatus)
                        .orderByAsc(Task::getDueTime));
        List<TaskVO> vos = page.getRecords().stream().map(this::toVO).toList();
        fillOwnerNames(vos, page.getRecords());
        return new PageResult<>(vos, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    /**
     * 我的待办：未完成且 due_time 临近（3 天内）或已过期，按截止时间升序。
     */
    public PageResult<TaskVO> todo(long pageNum, long pageSize) {
        LoginUser user = LoginUserContext.require();
        LocalDateTime now = LocalDateTime.now();
        Page<Task> page = page(Page.of(pageNum, pageSize),
                Wrappers.<Task>lambdaQuery()
                        .eq(Task::getOwnerId, user.userId())
                        .eq(Task::getStatus, 0)
                        .le(Task::getDueTime, now.plusDays(3))
                        .orderByAsc(Task::getDueTime));
        List<TaskVO> vos = page.getRecords().stream().map(this::toVO).toList();
        fillOwnerNames(vos, page.getRecords());
        return new PageResult<>(vos, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    private TaskVO toVO(Task task) {
        TaskVO vo = new TaskVO();
        vo.setId(task.getId());
        vo.setTitle(task.getTitle());
        vo.setContent(task.getContent());
        vo.setDueTime(task.getDueTime());
        boolean expired = task.getStatus() == 0 && task.getDueTime().isBefore(LocalDateTime.now());
        vo.setExpired(expired);
        int status = task.getStatus() == 1 ? 1 : (expired ? 2 : 0);
        vo.setStatus(status);
        vo.setStatusName(status == 1 ? "已完成" : status == 2 ? "已过期" : "待办");
        vo.setBizType(task.getBizType());
        vo.setBizTypeName(FollowBizTypes.labelOf(task.getBizType()));
        vo.setBizId(task.getBizId());
        vo.setOwnerId(task.getOwnerId());
        vo.setSource(task.getSource());
        vo.setCreateTime(task.getCreateTime());
        vo.setUpdateTime(task.getUpdateTime());
        return vo;
    }

    private void fillOwnerNames(List<TaskVO> vos, List<Task> tasks) {
        List<Long> ownerIds = tasks.stream().map(Task::getOwnerId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, String> nameMap = ownerIds.isEmpty() ? Map.of()
                : sysUserMapper.selectBatchIds(ownerIds).stream()
                .collect(Collectors.toMap(u -> u.getId(), u -> u.getNickname()));
        for (TaskVO vo : vos) {
            vo.setOwnerName(vo.getOwnerId() == null ? null : nameMap.get(vo.getOwnerId()));
        }
    }
}
