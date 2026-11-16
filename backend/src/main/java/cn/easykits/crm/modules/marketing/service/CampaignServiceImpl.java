package cn.easykits.crm.modules.marketing.service;

import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.ResultCode;
import cn.easykits.crm.common.context.LoginUser;
import cn.easykits.crm.common.context.LoginUserContext;
import cn.easykits.crm.common.exception.BizException;
import cn.easykits.crm.entity.Campaign;
import cn.easykits.crm.entity.SysUser;
import cn.easykits.crm.mapper.CampaignMapper;
import cn.easykits.crm.mapper.SysUserMapper;
import cn.easykits.crm.modules.marketing.dto.CampaignForm;
import cn.easykits.crm.modules.marketing.dto.CampaignQuery;
import cn.easykits.crm.modules.marketing.vo.CampaignVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 营销活动服务：CRUD / 分页 / 活动效果（线索归因）。
 *
 * <p>口径说明：
 * <ul>
 *   <li>线索数 / 转化数：crm_lead.campaign_id 匹配的线索，转化以 converted_customer_id 非空判定；</li>
 *   <li>商机金额：线索转化出的商机 amount 合计；赢单金额：阶段配置 is_won=1 的 actual_amount 合计；</li>
 *   <li>ROI = 赢单金额 / 预算，目标达成率 = 线索数 / 目标线索数，分母为 0 时返回 null（前端展示「—」）。</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class CampaignServiceImpl extends ServiceImpl<CampaignMapper, Campaign> {

    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private static final Map<Integer, String> TYPE_NAMES = Map.of(
            1, "线上推广", 2, "线下活动", 3, "电话营销", 4, "渠道合作", 5, "内容营销");

    private static final Map<Integer, String> STATUS_NAMES = Map.of(
            0, "草稿", 1, "进行中", 2, "已结束", 3, "已暂停");

    private final SysUserMapper sysUserMapper;

    @Transactional(rollbackFor = Exception.class)
    public Long createFromForm(CampaignForm form) {
        assertDateRange(form.getStartDate(), form.getEndDate());
        LoginUser user = LoginUserContext.require();
        Campaign entity = new Campaign();
        applyForm(entity, form);
        entity.setCode(resolveCode(form.getCode(), null));
        entity.setType(form.getType() == null ? 1 : form.getType());
        entity.setStatus(form.getStatus() == null ? 0 : form.getStatus());
        entity.setBudget(form.getBudget() == null ? BigDecimal.ZERO : form.getBudget());
        entity.setTargetLeads(form.getTargetLeads() == null ? 0 : form.getTargetLeads());
        entity.setOwnerId(user.userId());
        entity.setDeptId(user.deptId());
        save(entity);
        return entity.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateFromForm(Long id, CampaignForm form) {
        Campaign entity = getById(id);
        if (entity == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "营销活动不存在");
        }
        assertDateRange(form.getStartDate(), form.getEndDate());
        applyForm(entity, form);
        entity.setCode(resolveCode(form.getCode(), id));
        entity.setType(form.getType() == null ? entity.getType() : form.getType());
        entity.setStatus(form.getStatus() == null ? entity.getStatus() : form.getStatus());
        entity.setBudget(form.getBudget() == null ? entity.getBudget() : form.getBudget());
        entity.setTargetLeads(form.getTargetLeads() == null ? entity.getTargetLeads() : form.getTargetLeads());
        if (!updateById(entity)) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Campaign entity = getById(id);
        if (entity == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "营销活动不存在");
        }
        removeById(id);
    }

    public CampaignVO detail(Long id) {
        Campaign entity = getById(id);
        if (entity == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "营销活动不存在");
        }
        CampaignVO vo = toVO(entity);
        fillStats(List.of(vo));
        fillOwnerNames(List.of(vo));
        return vo;
    }

    public PageResult<CampaignVO> pageQuery(CampaignQuery query) {
        String keyword = StringUtils.hasText(query.getKeyword()) ? query.getKeyword().trim() : null;
        Page<Campaign> page = page(Page.of(query.getPageNum(), query.getPageSize()),
                Wrappers.<Campaign>lambdaQuery()
                        .and(keyword != null, w -> w.like(Campaign::getName, keyword)
                                .or().like(Campaign::getCode, keyword))
                        .eq(query.getType() != null, Campaign::getType, query.getType())
                        .eq(query.getStatus() != null, Campaign::getStatus, query.getStatus())
                        .orderByDesc(Campaign::getId));
        List<CampaignVO> vos = page.getRecords().stream().map(this::toVO).toList();
        fillStats(vos);
        fillOwnerNames(vos);
        return new PageResult<>(vos, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    /** 下拉选项（线索表单 / 报表筛选使用）。 */
    public List<Map<String, Object>> options() {
        return baseMapper.options();
    }

    // ============================================================ 内部方法

    private void applyForm(Campaign entity, CampaignForm form) {
        entity.setName(form.getName());
        entity.setChannel(form.getChannel());
        entity.setStartDate(form.getStartDate());
        entity.setEndDate(form.getEndDate());
        entity.setRemark(form.getRemark());
    }

    private void assertDateRange(LocalDate start, LocalDate end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new BizException("开始日期不能晚于结束日期");
        }
    }

    private String resolveCode(String code, Long excludeId) {
        if (!StringUtils.hasText(code)) {
            return "MC-" + LocalDateTime.now().format(NO_FMT) + "-"
                    + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        }
        Campaign same = getOne(Wrappers.<Campaign>lambdaQuery()
                .eq(Campaign::getCode, code)
                .ne(excludeId != null, Campaign::getId, excludeId)
                .last("limit 1"));
        if (same != null) {
            throw new BizException("活动编码已存在: " + code);
        }
        return code;
    }

    /** 批量补齐效果指标（三次聚合查询，避免逐行查库）。 */
    private void fillStats(List<CampaignVO> vos) {
        if (vos.isEmpty()) {
            return;
        }
        List<Long> ids = vos.stream().map(CampaignVO::getId).filter(Objects::nonNull).toList();
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, Map<String, Object>> leadMap = new HashMap<>();
        for (Map<String, Object> row : baseMapper.leadStats(ids)) {
            leadMap.put(toLong(row.get("campaignId")), row);
        }
        Map<Long, Map<String, Object>> oppMap = new HashMap<>();
        for (Map<String, Object> row : baseMapper.opportunityStats(ids)) {
            oppMap.put(toLong(row.get("campaignId")), row);
        }
        for (CampaignVO vo : vos) {
            Map<String, Object> lead = leadMap.getOrDefault(vo.getId(), Map.of());
            Map<String, Object> opp = oppMap.getOrDefault(vo.getId(), Map.of());
            long leadCount = toLong(lead.get("leadCount"));
            long converted = toLong(lead.get("convertedCount"));
            BigDecimal pipeline = toDecimal(opp.get("pipelineAmount"));
            BigDecimal won = toDecimal(opp.get("wonAmount"));
            vo.setLeadCount(leadCount);
            vo.setConvertedCount(converted);
            vo.setConversionRate(rate(BigDecimal.valueOf(converted), BigDecimal.valueOf(leadCount)));
            vo.setPipelineAmount(pipeline);
            vo.setWonAmount(won);
            vo.setRoi(rate(won, vo.getBudget()));
            vo.setTargetRate(rate(BigDecimal.valueOf(leadCount),
                    vo.getTargetLeads() == null ? null : BigDecimal.valueOf(vo.getTargetLeads())));
        }
    }

    private void fillOwnerNames(List<CampaignVO> vos) {
        List<Long> ownerIds = vos.stream().map(CampaignVO::getOwnerId)
                .filter(Objects::nonNull).distinct().toList();
        if (ownerIds.isEmpty()) {
            return;
        }
        Map<Long, String> nameMap = sysUserMapper.selectBatchIds(ownerIds).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getNickname));
        for (CampaignVO vo : vos) {
            vo.setOwnerName(vo.getOwnerId() == null ? null : nameMap.get(vo.getOwnerId()));
        }
    }

    /** 比率（保留 2 位小数）；分母为空或 0 时返回 null。 */
    private BigDecimal rate(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.signum() == 0) {
            return null;
        }
        return numerator.divide(denominator, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        return ((Number) value).longValue();
    }

    private BigDecimal toDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value.toString());
    }

    private CampaignVO toVO(Campaign entity) {
        CampaignVO vo = new CampaignVO();
        vo.setId(entity.getId());
        vo.setCode(entity.getCode());
        vo.setName(entity.getName());
        vo.setType(entity.getType());
        vo.setTypeName(TYPE_NAMES.getOrDefault(entity.getType(), "未知"));
        vo.setChannel(entity.getChannel());
        vo.setStatus(entity.getStatus());
        vo.setStatusName(STATUS_NAMES.getOrDefault(entity.getStatus(), "未知"));
        vo.setBudget(entity.getBudget());
        vo.setStartDate(entity.getStartDate());
        vo.setEndDate(entity.getEndDate());
        vo.setTargetLeads(entity.getTargetLeads());
        vo.setOwnerId(entity.getOwnerId());
        vo.setDeptId(entity.getDeptId());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
