package cn.easykits.crm.modules.payment.service;

import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.ResultCode;
import cn.easykits.crm.common.exception.BizException;
import cn.easykits.crm.entity.PaymentPlan;
import cn.easykits.crm.mapper.PaymentPlanMapper;
import cn.easykits.crm.modules.payment.constant.PaymentDicts;
import cn.easykits.crm.modules.payment.dto.PaymentPlanForm;
import cn.easykits.crm.modules.payment.vo.PaymentPlanVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 回款计划服务：同订单期次唯一、已核销计划保护、CRUD / 分页。
 */
@Service
@RequiredArgsConstructor
public class PaymentPlanServiceImpl extends ServiceImpl<PaymentPlanMapper, PaymentPlan> {

    @Transactional(rollbackFor = Exception.class)
    public Long createFromForm(PaymentPlanForm form) {
        assertPeriodUnique(form.getOrderId(), form.getPeriodNo(), null);
        PaymentPlan entity = new PaymentPlan();
        applyForm(entity, form);
        entity.setReceivedAmount(BigDecimal.ZERO);
        entity.setStatus(0);
        save(entity);
        return entity.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateFromForm(Long id, PaymentPlanForm form) {
        PaymentPlan entity = getById(id);
        if (entity == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "回款计划不存在");
        }
        if (entity.getReceivedAmount() != null && entity.getReceivedAmount().signum() > 0) {
            throw new BizException("该计划已有核销记录，不可修改");
        }
        assertPeriodUnique(form.getOrderId(), form.getPeriodNo(), id);
        applyForm(entity, form);
        if (!updateById(entity)) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        PaymentPlan entity = getById(id);
        if (entity == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "回款计划不存在");
        }
        if (entity.getReceivedAmount() != null && entity.getReceivedAmount().signum() > 0) {
            throw new BizException("该计划已有核销记录，不可删除");
        }
        removeById(id);
    }

    public PaymentPlanVO detail(Long id) {
        PaymentPlan entity = getById(id);
        if (entity == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "回款计划不存在");
        }
        return toVO(entity);
    }

    public PageResult<PaymentPlanVO> pageQuery(long pageNum, long pageSize, Long orderId, Integer status) {
        Page<PaymentPlan> page = page(Page.of(pageNum, pageSize),
                Wrappers.<PaymentPlan>lambdaQuery()
                        .eq(orderId != null, PaymentPlan::getOrderId, orderId)
                        .eq(status != null, PaymentPlan::getStatus, status)
                        .orderByAsc(PaymentPlan::getOrderId)
                        .orderByAsc(PaymentPlan::getPeriodNo));
        return PageResult.of(page, this::toVO);
    }

    private void assertPeriodUnique(Long orderId, Integer periodNo, Long excludeId) {
        PaymentPlan same = getOne(Wrappers.<PaymentPlan>lambdaQuery()
                .eq(PaymentPlan::getOrderId, orderId)
                .eq(PaymentPlan::getPeriodNo, periodNo)
                .ne(excludeId != null, PaymentPlan::getId, excludeId)
                .last("limit 1"));
        if (same != null) {
            throw new BizException("该订单已存在第 %d 期回款计划".formatted(periodNo));
        }
    }

    private void applyForm(PaymentPlan entity, PaymentPlanForm form) {
        entity.setOrderId(form.getOrderId());
        entity.setPeriodNo(form.getPeriodNo());
        entity.setPlanDate(form.getPlanDate());
        entity.setPlanAmount(form.getPlanAmount());
        entity.setRemark(form.getRemark());
    }

    private PaymentPlanVO toVO(PaymentPlan entity) {
        PaymentPlanVO vo = new PaymentPlanVO();
        vo.setId(entity.getId());
        vo.setOrderId(entity.getOrderId());
        vo.setPeriodNo(entity.getPeriodNo());
        vo.setPlanDate(entity.getPlanDate());
        vo.setPlanAmount(entity.getPlanAmount());
        vo.setReceivedAmount(entity.getReceivedAmount());
        vo.setStatus(entity.getStatus());
        vo.setStatusName(PaymentDicts.planStatusName(entity.getStatus()));
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
