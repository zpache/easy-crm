package cn.easykits.crm.modules.report.service;

import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.ResultCode;
import cn.easykits.crm.common.exception.BizException;
import cn.easykits.crm.entity.SalesTarget;
import cn.easykits.crm.entity.SysDept;
import cn.easykits.crm.entity.SysUser;
import cn.easykits.crm.mapper.SalesTargetMapper;
import cn.easykits.crm.mapper.SysDeptMapper;
import cn.easykits.crm.mapper.SysUserMapper;
import cn.easykits.crm.modules.report.dto.SalesTargetForm;
import cn.easykits.crm.modules.report.dto.SalesTargetQuery;
import cn.easykits.crm.modules.report.vo.SalesTargetVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 销售目标服务：CRUD / 分页，唯一性口径 (target_month, dept_id, owner_id)。
 * <p>注意：MySQL 唯一索引对 NULL 不去重，故唯一性在应用层显式校验。
 */
@Service
@RequiredArgsConstructor
public class SalesTargetServiceImpl extends ServiceImpl<SalesTargetMapper, SalesTarget> {

    private final SysDeptMapper sysDeptMapper;
    private final SysUserMapper sysUserMapper;

    @Transactional(rollbackFor = Exception.class)
    public Long createFromForm(SalesTargetForm form) {
        assertUnique(form.getTargetMonth(), form.getDeptId(), form.getOwnerId(), null);
        SalesTarget entity = new SalesTarget();
        applyForm(entity, form);
        save(entity);
        return entity.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateFromForm(Long id, SalesTargetForm form) {
        SalesTarget entity = getById(id);
        if (entity == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "销售目标不存在");
        }
        assertUnique(form.getTargetMonth(), form.getDeptId(), form.getOwnerId(), id);
        applyForm(entity, form);
        if (!updateById(entity)) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SalesTarget entity = getById(id);
        if (entity == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "销售目标不存在");
        }
        removeById(id);
    }

    public SalesTargetVO detail(Long id) {
        SalesTarget entity = getById(id);
        if (entity == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "销售目标不存在");
        }
        SalesTargetVO vo = toVO(entity);
        fillNames(List.of(vo));
        return vo;
    }

    public PageResult<SalesTargetVO> pageQuery(SalesTargetQuery query) {
        Page<SalesTarget> page = page(Page.of(query.getPageNum(), query.getPageSize()),
                Wrappers.<SalesTarget>lambdaQuery()
                        .eq(StringUtils.hasText(query.getTargetMonth()),
                                SalesTarget::getTargetMonth, query.getTargetMonth())
                        .eq(query.getDeptId() != null, SalesTarget::getDeptId, query.getDeptId())
                        .eq(query.getOwnerId() != null, SalesTarget::getOwnerId, query.getOwnerId())
                        .orderByDesc(SalesTarget::getTargetMonth)
                        .orderByDesc(SalesTarget::getId));
        PageResult<SalesTargetVO> result = PageResult.of(page, this::toVO);
        fillNames(result.records());
        return result;
    }

    private void assertUnique(String month, Long deptId, Long ownerId, Long excludeId) {
        SalesTarget same = getOne(Wrappers.<SalesTarget>lambdaQuery()
                .eq(SalesTarget::getTargetMonth, month)
                .eq(deptId != null, SalesTarget::getDeptId, deptId)
                .isNull(deptId == null, SalesTarget::getDeptId)
                .eq(ownerId != null, SalesTarget::getOwnerId, ownerId)
                .isNull(ownerId == null, SalesTarget::getOwnerId)
                .ne(excludeId != null, SalesTarget::getId, excludeId)
                .last("limit 1"));
        if (same != null) {
            throw new BizException("该年月 + 部门 + 负责人的目标已存在，请勿重复新增");
        }
    }

    private void applyForm(SalesTarget entity, SalesTargetForm form) {
        entity.setTargetMonth(form.getTargetMonth());
        entity.setDeptId(form.getDeptId());
        entity.setOwnerId(form.getOwnerId());
        entity.setTargetAmount(form.getTargetAmount());
        entity.setRemark(form.getRemark());
    }

    private void fillNames(List<SalesTargetVO> vos) {
        List<Long> deptIds = vos.stream().map(SalesTargetVO::getDeptId)
                .filter(Objects::nonNull).distinct().toList();
        List<Long> ownerIds = vos.stream().map(SalesTargetVO::getOwnerId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, String> deptMap = deptIds.isEmpty() ? Map.of()
                : sysDeptMapper.selectBatchIds(deptIds).stream()
                .collect(Collectors.toMap(SysDept::getId, SysDept::getName));
        Map<Long, String> ownerMap = ownerIds.isEmpty() ? Map.of()
                : sysUserMapper.selectBatchIds(ownerIds).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getNickname));
        for (SalesTargetVO vo : vos) {
            vo.setDeptName(vo.getDeptId() == null ? null : deptMap.get(vo.getDeptId()));
            vo.setOwnerName(vo.getOwnerId() == null ? null : ownerMap.get(vo.getOwnerId()));
        }
    }

    private SalesTargetVO toVO(SalesTarget entity) {
        SalesTargetVO vo = new SalesTargetVO();
        vo.setId(entity.getId());
        vo.setTargetMonth(entity.getTargetMonth());
        vo.setDeptId(entity.getDeptId());
        vo.setOwnerId(entity.getOwnerId());
        vo.setTargetAmount(entity.getTargetAmount());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
