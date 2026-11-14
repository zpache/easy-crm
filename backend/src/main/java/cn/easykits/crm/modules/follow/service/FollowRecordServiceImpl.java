package cn.easykits.crm.modules.follow.service;

import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.ResultCode;
import cn.easykits.crm.common.context.LoginUser;
import cn.easykits.crm.common.context.LoginUserContext;
import cn.easykits.crm.common.exception.BizException;
import cn.easykits.crm.entity.FollowRecord;
import cn.easykits.crm.entity.Task;
import cn.easykits.crm.mapper.FollowRecordMapper;
import cn.easykits.crm.mapper.TaskMapper;
import cn.easykits.crm.modules.follow.constant.FollowBizTypes;
import cn.easykits.crm.modules.follow.constant.FollowTypes;
import cn.easykits.crm.modules.follow.dto.FollowRecordCreateForm;
import cn.easykits.crm.modules.follow.dto.FollowRecordQuery;
import cn.easykits.crm.modules.follow.dto.FollowRecordUpdateForm;
import cn.easykits.crm.modules.follow.strategy.FollowActivityUpdaterRegistry;
import cn.easykits.crm.modules.follow.vo.FollowRecordVO;
import cn.easykits.crm.modules.follow.vo.TimelineItemVO;
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
 * 跟进记录服务：CRUD + 策略分发更新业务对象活动时间 + 自动生成待办 + 时间线聚合。
 * <p>活动时间更新与记录写入在同一事务，保证一致。
 */
@Service
@RequiredArgsConstructor
public class FollowRecordServiceImpl extends ServiceImpl<FollowRecordMapper, FollowRecord> {

    private final FollowActivityUpdaterRegistry activityUpdaterRegistry;
    private final TaskMapper taskMapper;
    private final cn.easykits.crm.mapper.SysUserMapper sysUserMapper;
    private final cn.easykits.crm.mapper.OpportunityStageMapper stageMapper;

    /**
     * 创建跟进记录：写入 + 策略分发更新业务对象活动时间（同事务），
     * nextFollowTime 选填 → 自动生成待办任务（source=FOLLOW）。
     */
    @Transactional(rollbackFor = Exception.class)
    public FollowRecordVO create(FollowRecordCreateForm form) {
        if (!FollowBizTypes.isValid(form.getBizType())) {
            throw new BizException("业务类型不合法: " + form.getBizType());
        }
        if (!FollowTypes.isValid(form.getFollowType())) {
            throw new BizException("跟进类型不合法: " + form.getFollowType());
        }
        LoginUser user = LoginUserContext.require();
        FollowRecord record = new FollowRecord();
        record.setBizType(form.getBizType());
        record.setBizId(form.getBizId());
        record.setFollowType(form.getFollowType());
        record.setContent(form.getContent());
        record.setNextFollowTime(form.getNextFollowTime());
        record.setOwnerId(user.userId());
        record.setDeptId(user.deptId());
        save(record);

        // 策略分发：更新业务对象活动时间（同事务）
        activityUpdaterRegistry.updateActivity(form.getBizType(), form.getBizId());

        // 自动生成待办任务
        if (form.getNextFollowTime() != null) {
            Task task = new Task();
            task.setTitle("跟进回访：%s".formatted(
                    FollowBizTypes.labelOf(form.getBizType()) + " #" + form.getBizId()));
            task.setContent("由跟进记录（ID: %d）自动生成：%s".formatted(
                    record.getId(), StringUtils.hasText(form.getContent())
                            ? form.getContent().substring(0, Math.min(form.getContent().length(), 100)) : ""));
            task.setDueTime(form.getNextFollowTime());
            task.setStatus(0);
            task.setBizType(form.getBizType());
            task.setBizId(form.getBizId());
            task.setOwnerId(user.userId());
            task.setSource("FOLLOW");
            taskMapper.insert(task);
        }
        return toVO(record);
    }

    /**
     * 更新跟进记录：内容变更 + 活动时间刷新（同事务）；同步刷新关联待办截止时间。
     */
    @Transactional(rollbackFor = Exception.class)
    public FollowRecordVO update(Long id, FollowRecordUpdateForm form) {
        FollowRecord record = getById(id);
        if (record == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "跟进记录不存在");
        }
        if (!FollowTypes.isValid(form.getFollowType())) {
            throw new BizException("跟进类型不合法: " + form.getFollowType());
        }
        record.setFollowType(form.getFollowType());
        record.setContent(form.getContent());
        record.setNextFollowTime(form.getNextFollowTime());
        if (!updateById(record)) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
        // 同业务对象活动时间刷新（同事务）
        activityUpdaterRegistry.updateActivity(record.getBizType(), record.getBizId());
        // 同步该记录生成的待办任务截止时间
        if (form.getNextFollowTime() != null) {
            taskMapper.update(null, Wrappers.<Task>lambdaUpdate()
                    .eq(Task::getBizType, record.getBizType())
                    .eq(Task::getBizId, record.getBizId())
                    .eq(Task::getSource, "FOLLOW")
                    .eq(Task::getStatus, 0)
                    .set(Task::getDueTime, form.getNextFollowTime()));
        }
        return toVO(record);
    }

    /**
     * 删除跟进记录（逻辑删除）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        FollowRecord record = getById(id);
        if (record == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "跟进记录不存在");
        }
        removeById(id);
    }

    /**
     * 分页查询（bizType/bizId/ownerId/时间范围）。
     */
    public PageResult<FollowRecordVO> pageQuery(FollowRecordQuery query) {
        Page<FollowRecord> page = page(Page.of(query.getPageNum(), query.getPageSize()),
                Wrappers.<FollowRecord>lambdaQuery()
                        .eq(StringUtils.hasText(query.getBizType()), FollowRecord::getBizType, query.getBizType())
                        .eq(query.getBizId() != null, FollowRecord::getBizId, query.getBizId())
                        .isNull(query.getOwnerId() != null && query.getOwnerId() == -1, FollowRecord::getOwnerId)
                        .eq(query.getOwnerId() != null && query.getOwnerId() != -1,
                                FollowRecord::getOwnerId, query.getOwnerId())
                        .ge(query.getStartDate() != null, FollowRecord::getCreateTime, query.getStartDate())
                        .le(query.getEndDate() != null, FollowRecord::getCreateTime, query.getEndDate())
                        .orderByDesc(FollowRecord::getId));
        List<FollowRecordVO> vos = page.getRecords().stream().map(this::toVO).toList();
        fillOwnerNames(vos, page.getRecords());
        return new PageResult<>(vos, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    /**
     * 时间线聚合：一条 UNION ALL SQL（含分页）+ 一条计数 SQL，
     * Service 层仅做字典标签翻译与阶段名批量查询（查询次数固定）。
     */
    public PageResult<TimelineItemVO> timeline(String bizType, Long bizId, long pageNum, long pageSize) {
        if (!FollowBizTypes.isValid(bizType)) {
            throw new BizException("业务类型不合法: " + bizType);
        }
        long offset = (pageNum - 1) * pageSize;
        List<TimelineItemVO> items = baseMapper.timeline(bizType, bizId, offset, pageSize);
        long total = baseMapper.timelineCount(bizType, bizId);

        // 标签翻译：跟进类型 / 任务状态
        for (TimelineItemVO item : items) {
            if ("follow".equals(item.getSource())) {
                item.setTypeName(FollowTypes.labelOf(item.getTypeCode()));
            } else if ("task".equals(item.getSource())) {
                item.setTypeName("1".equals(item.getTypeCode()) ? "已完成" : "待办");
            }
        }
        // 阶段名批量查询（一次）
        List<String> stageCodes = items.stream()
                .flatMap(item -> java.util.stream.Stream.of(item.getFromStage(), item.getToStage()))
                .filter(StringUtils::hasText)
                .distinct().toList();
        if (!stageCodes.isEmpty()) {
            Map<String, String> stageNameMap = stageMapper.selectList(
                            Wrappers.<cn.easykits.crm.entity.OpportunityStage>lambdaQuery()
                                    .in(cn.easykits.crm.entity.OpportunityStage::getStageCode, stageCodes))
                    .stream()
                    .collect(Collectors.toMap(
                            cn.easykits.crm.entity.OpportunityStage::getStageCode,
                            cn.easykits.crm.entity.OpportunityStage::getStageName));
            for (TimelineItemVO item : items) {
                item.setFromStageName(stageNameMap.get(item.getFromStage()));
                item.setToStageName(stageNameMap.get(item.getToStage()));
                if ("stage_log".equals(item.getSource())) {
                    item.setTypeName("%s → %s".formatted(
                            item.getFromStageName() == null ? "创建" : item.getFromStageName(),
                            item.getToStageName() == null ? "-" : item.getToStageName()));
                    if (!StringUtils.hasText(item.getContent())) {
                        item.setContent("商机阶段变更");
                    }
                }
            }
        }
        // 操作人名称批量查询（一次）
        List<Long> operatorIds = items.stream().map(TimelineItemVO::getOperatorId)
                .filter(Objects::nonNull).distinct().toList();
        if (!operatorIds.isEmpty()) {
            Map<Long, String> nameMap = sysUserMapper.selectBatchIds(operatorIds).stream()
                    .collect(Collectors.toMap(u -> u.getId(), u -> u.getNickname()));
            for (TimelineItemVO item : items) {
                item.setOperatorName(item.getOperatorId() == null ? null
                        : nameMap.get(item.getOperatorId()));
            }
        }
        return new PageResult<>(items, total, pageNum, pageSize,
                (total + pageSize - 1) / pageSize);
    }

    private FollowRecordVO toVO(FollowRecord record) {
        FollowRecordVO vo = new FollowRecordVO();
        vo.setId(record.getId());
        vo.setBizType(record.getBizType());
        vo.setBizTypeName(FollowBizTypes.labelOf(record.getBizType()));
        vo.setBizId(record.getBizId());
        vo.setFollowType(record.getFollowType());
        vo.setFollowTypeName(FollowTypes.labelOf(record.getFollowType()));
        vo.setContent(record.getContent());
        vo.setNextFollowTime(record.getNextFollowTime());
        vo.setOwnerId(record.getOwnerId());
        vo.setDeptId(record.getDeptId());
        vo.setCreateTime(record.getCreateTime());
        vo.setUpdateTime(record.getUpdateTime());
        return vo;
    }

    private void fillOwnerNames(List<FollowRecordVO> vos, List<FollowRecord> records) {
        List<Long> ownerIds = records.stream().map(FollowRecord::getOwnerId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, String> nameMap = ownerIds.isEmpty() ? Map.of()
                : sysUserMapper.selectBatchIds(ownerIds).stream()
                .collect(Collectors.toMap(u -> u.getId(), u -> u.getNickname()));
        for (FollowRecordVO vo : vos) {
            vo.setOwnerName(vo.getOwnerId() == null ? null : nameMap.get(vo.getOwnerId()));
        }
    }
}
