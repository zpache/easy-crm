package cn.easykits.crm.modules.support.service;

import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.ResultCode;
import cn.easykits.crm.common.exception.BizException;
import cn.easykits.crm.entity.SlaPolicy;
import cn.easykits.crm.mapper.SlaPolicyMapper;
import cn.easykits.crm.modules.support.constant.SupportDicts;
import cn.easykits.crm.modules.support.dto.SlaPolicyForm;
import cn.easykits.crm.modules.support.dto.SlaPolicyQuery;
import cn.easykits.crm.modules.support.vo.SlaPolicyVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * SLA 策略服务：配置表 CRUD + 启停。crm_sla_policy 为全局配置，不接行级数据权限。
 * (ticket_type, priority) 组合唯一，由数据库 uk_type_priority 与业务层双重保证。
 */
@Service
@RequiredArgsConstructor
public class SlaPolicyServiceImpl extends ServiceImpl<SlaPolicyMapper, SlaPolicy> {

    /** 分页查询（类型 / 优先级 / 启停）。 */
    public PageResult<SlaPolicyVO> pageQuery(SlaPolicyQuery query) {
        Page<SlaPolicy> page = page(Page.of(query.getPageNum(), query.getPageSize()),
                Wrappers.<SlaPolicy>lambdaQuery()
                        .eq(StringUtils.hasText(query.getTicketType()), SlaPolicy::getTicketType, query.getTicketType())
                        .eq(StringUtils.hasText(query.getPriority()), SlaPolicy::getPriority, query.getPriority())
                        .eq(query.getEnabled() != null, SlaPolicy::getEnabled, query.getEnabled())
                        .orderByAsc(SlaPolicy::getTicketType)
                        .orderByAsc(SlaPolicy::getId));
        List<SlaPolicyVO> vos = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(vos, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    /** 详情。 */
    public SlaPolicyVO detail(Long id) {
        SlaPolicy policy = getById(id);
        if (policy == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "SLA 策略不存在");
        }
        return toVO(policy);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(SlaPolicyForm form) {
        validateDict(form);
        assertPairAvailable(form.getTicketType(), form.getPriority(), null);
        SlaPolicy policy = new SlaPolicy();
        applyForm(policy, form);
        save(policy);
        return policy.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SlaPolicyForm form) {
        SlaPolicy policy = getById(id);
        if (policy == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "SLA 策略不存在");
        }
        validateDict(form);
        assertPairAvailable(form.getTicketType(), form.getPriority(), id);
        applyForm(policy, form);
        if (!updateById(policy)) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    /** 删除（逻辑删除）。注意：逻辑删除行仍占用 uk_type_priority，见 INTEGRATION.md 已知限制。 */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SlaPolicy policy = getById(id);
        if (policy == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "SLA 策略不存在");
        }
        removeById(id);
    }

    private void validateDict(SlaPolicyForm form) {
        if (!SupportDicts.isValidType(form.getTicketType())) {
            throw new BizException("工单类型不合法: " + form.getTicketType());
        }
        if (!SupportDicts.isValidPriority(form.getPriority())) {
            throw new BizException("优先级不合法: " + form.getPriority());
        }
        if (form.getResolveHours() < form.getResponseHours()) {
            throw new BizException("解决小时数不能小于响应小时数");
        }
    }

    private void assertPairAvailable(String ticketType, String priority, Long excludeId) {
        Long count = baseMapper.selectCount(Wrappers.<SlaPolicy>lambdaQuery()
                .eq(SlaPolicy::getTicketType, ticketType)
                .eq(SlaPolicy::getPriority, priority)
                .ne(excludeId != null, SlaPolicy::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException("该工单类型 + 优先级的 SLA 策略已存在");
        }
    }

    private void applyForm(SlaPolicy policy, SlaPolicyForm form) {
        policy.setTicketType(form.getTicketType());
        policy.setPriority(form.getPriority());
        policy.setResponseHours(form.getResponseHours());
        policy.setResolveHours(form.getResolveHours());
        policy.setEnabled(form.getEnabled() == null ? 1 : form.getEnabled());
    }

    private SlaPolicyVO toVO(SlaPolicy policy) {
        SlaPolicyVO vo = new SlaPolicyVO();
        vo.setId(policy.getId());
        vo.setTicketType(policy.getTicketType());
        vo.setTicketTypeName(SupportDicts.typeLabel(policy.getTicketType()));
        vo.setPriority(policy.getPriority());
        vo.setPriorityName(SupportDicts.priorityLabel(policy.getPriority()));
        vo.setResponseHours(policy.getResponseHours());
        vo.setResolveHours(policy.getResolveHours());
        vo.setEnabled(policy.getEnabled());
        vo.setCreateTime(policy.getCreateTime());
        vo.setUpdateTime(policy.getUpdateTime());
        return vo;
    }
}
