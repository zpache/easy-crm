package cn.easykits.crm.modules.support.service;

import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.ResultCode;
import cn.easykits.crm.common.exception.BizException;
import cn.easykits.crm.entity.Satisfaction;
import cn.easykits.crm.entity.ServiceTicket;
import cn.easykits.crm.entity.SysUser;
import cn.easykits.crm.mapper.SatisfactionMapper;
import cn.easykits.crm.mapper.ServiceTicketMapper;
import cn.easykits.crm.mapper.SysUserMapper;
import cn.easykits.crm.modules.support.constant.TicketStatus;
import cn.easykits.crm.modules.support.dto.SatisfactionForm;
import cn.easykits.crm.modules.support.dto.SatisfactionQuery;
import cn.easykits.crm.modules.support.vo.SatisfactionStatsVO;
import cn.easykits.crm.modules.support.vo.SatisfactionVO;
import cn.easykits.crm.modules.support.vo.ScoreCountVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 满意度评价服务：工单完成时允许评价、一单一评；分页列表 + 评分分布统计。
 * 行级数据权限：crm_satisfaction 需登记到 DataScopeTableRegistry（owner_id + dept_id）。
 */
@Service
@RequiredArgsConstructor
public class SatisfactionServiceImpl extends ServiceImpl<SatisfactionMapper, Satisfaction> {

    private final ServiceTicketMapper serviceTicketMapper;
    private final SysUserMapper sysUserMapper;

    /** 评价：工单必须存在且已完成（COMPLETED）；一单一评。 */
    @Transactional(rollbackFor = Exception.class)
    public Long create(SatisfactionForm form) {
        ServiceTicket ticket = serviceTicketMapper.selectById(form.getTicketId());
        if (ticket == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "工单不存在");
        }
        if (!TicketStatus.COMPLETED.code().equals(ticket.getStatus())) {
            throw new BizException("仅已完成（COMPLETED）工单可评价");
        }
        Long exists = baseMapper.selectCount(Wrappers.<Satisfaction>lambdaQuery()
                .eq(Satisfaction::getTicketId, form.getTicketId()));
        if (exists != null && exists > 0) {
            throw new BizException("该工单已评价，一单一评");
        }
        Satisfaction satisfaction = new Satisfaction();
        satisfaction.setTicketId(form.getTicketId());
        satisfaction.setScore(form.getScore());
        satisfaction.setContent(form.getContent());
        satisfaction.setEvaluateTime(LocalDateTime.now());
        // 冗余工单归属，供行级数据权限过滤
        satisfaction.setOwnerId(ticket.getOwnerId());
        satisfaction.setDeptId(ticket.getDeptId());
        save(satisfaction);
        return satisfaction.getId();
    }

    /** 分页列表（工单 / 评分 / 评价时间范围）。 */
    public PageResult<SatisfactionVO> pageQuery(SatisfactionQuery query) {
        Page<Satisfaction> page = page(Page.of(query.getPageNum(), query.getPageSize()),
                Wrappers.<Satisfaction>lambdaQuery()
                        .eq(query.getTicketId() != null, Satisfaction::getTicketId, query.getTicketId())
                        .eq(query.getScore() != null, Satisfaction::getScore, query.getScore())
                        .ge(query.getStartDate() != null, Satisfaction::getEvaluateTime, query.getStartDate())
                        .le(query.getEndDate() != null, Satisfaction::getEvaluateTime, query.getEndDate())
                        .orderByDesc(Satisfaction::getId));
        List<SatisfactionVO> vos = page.getRecords().stream().map(this::toVO).toList();
        fillNames(vos, page.getRecords());
        return new PageResult<>(vos, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    /**
     * 统计：一条聚合 SQL 取评分分布（可选评价时间范围），Service 层汇总总数与平均分，
     * 并把 1-5 分缺失档补 0，便于前端直接渲染表格 / 图表。
     */
    public SatisfactionStatsVO stats(SatisfactionQuery query) {
        List<ScoreCountVO> distribution = baseMapper.selectScoreDistribution(
                query.getStartDate(), query.getEndDate());
        Map<Integer, Long> countMap = distribution.stream()
                .collect(Collectors.toMap(ScoreCountVO::getScore, ScoreCountVO::getCnt, (a, b) -> a));
        long total = 0L;
        BigDecimal weighted = BigDecimal.ZERO;
        List<ScoreCountVO> full = new ArrayList<>();
        for (int score = 1; score <= 5; score++) {
            long cnt = countMap.getOrDefault(score, 0L);
            ScoreCountVO item = new ScoreCountVO();
            item.setScore(score);
            item.setCnt(cnt);
            full.add(item);
            total += cnt;
            weighted = weighted.add(BigDecimal.valueOf((long) score * cnt));
        }
        BigDecimal average = total == 0 ? BigDecimal.ZERO
                : weighted.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        SatisfactionStatsVO vo = new SatisfactionStatsVO();
        vo.setTotal(total);
        vo.setAverageScore(average);
        vo.setDistribution(full);
        return vo;
    }

    private SatisfactionVO toVO(Satisfaction satisfaction) {
        SatisfactionVO vo = new SatisfactionVO();
        vo.setId(satisfaction.getId());
        vo.setTicketId(satisfaction.getTicketId());
        vo.setScore(satisfaction.getScore());
        vo.setContent(satisfaction.getContent());
        vo.setEvaluateTime(satisfaction.getEvaluateTime());
        vo.setOwnerId(satisfaction.getOwnerId());
        vo.setCreateTime(satisfaction.getCreateTime());
        return vo;
    }

    /** 批量补工单编号与受理人名称。 */
    private void fillNames(List<SatisfactionVO> vos, List<Satisfaction> records) {
        List<Long> ticketIds = records.stream().map(Satisfaction::getTicketId)
                .filter(Objects::nonNull).distinct().toList();
        List<Long> ownerIds = records.stream().map(Satisfaction::getOwnerId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, String> ticketNoMap = ticketIds.isEmpty() ? Map.of()
                : serviceTicketMapper.selectBatchIds(ticketIds).stream()
                .collect(Collectors.toMap(ServiceTicket::getId, ServiceTicket::getTicketNo));
        Map<Long, String> ownerMap = ownerIds.isEmpty() ? Map.of()
                : sysUserMapper.selectBatchIds(ownerIds).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getNickname, (a, b) -> a));
        for (SatisfactionVO vo : vos) {
            vo.setTicketNo(ticketNoMap.get(vo.getTicketId()));
            vo.setOwnerName(vo.getOwnerId() == null ? null : ownerMap.get(vo.getOwnerId()));
        }
    }
}
