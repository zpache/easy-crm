package cn.easykits.crm.modules.payment.service;

import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.ResultCode;
import cn.easykits.crm.common.exception.BizException;
import cn.easykits.crm.entity.Payment;
import cn.easykits.crm.entity.PaymentPlan;
import cn.easykits.crm.mapper.PaymentMapper;
import cn.easykits.crm.mapper.PaymentPlanMapper;
import cn.easykits.crm.modules.payment.constant.PaymentDicts;
import cn.easykits.crm.modules.payment.dto.PaymentForm;
import cn.easykits.crm.modules.payment.dto.PaymentQuery;
import cn.easykits.crm.modules.payment.vo.PaymentVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 回款记录服务：CRUD / 分页 / 核销。
 * 核销在单事务内完成：累加计划 receivedAmount、重算计划状态、回写支付记录核销标记。
 */
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl extends ServiceImpl<PaymentMapper, Payment> {

    private final PaymentPlanMapper paymentPlanMapper;

    @Transactional(rollbackFor = Exception.class)
    public Long createFromForm(PaymentForm form) {
        if (form.getPlanId() != null) {
            assertPlanInOrder(form.getPlanId(), form.getOrderId());
        }
        Payment entity = new Payment();
        applyForm(entity, form);
        entity.setWriteoffStatus(0);
        save(entity);
        return entity.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateFromForm(Long id, PaymentForm form) {
        Payment entity = getById(id);
        if (entity == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "回款记录不存在");
        }
        if (isWrittenOff(entity)) {
            throw new BizException("该回款已核销，不可修改");
        }
        if (form.getPlanId() != null) {
            assertPlanInOrder(form.getPlanId(), form.getOrderId());
        }
        applyForm(entity, form);
        if (!updateById(entity)) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Payment entity = getById(id);
        if (entity == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "回款记录不存在");
        }
        if (isWrittenOff(entity)) {
            throw new BizException("该回款已核销，不可删除");
        }
        removeById(id);
    }

    /** 核销：把本笔实收金额计入指定回款计划。 */
    @Transactional(rollbackFor = Exception.class)
    public void writeoff(Long id, Long planId) {
        Payment payment = getById(id);
        if (payment == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "回款记录不存在");
        }
        if (isWrittenOff(payment)) {
            throw new BizException("该回款已核销，请勿重复操作");
        }
        PaymentPlan plan = paymentPlanMapper.selectById(planId);
        if (plan == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "回款计划不存在");
        }
        if (!Objects.equals(plan.getOrderId(), payment.getOrderId())) {
            throw new BizException("回款计划与回款记录不属于同一订单");
        }

        BigDecimal received = plan.getReceivedAmount() == null ? BigDecimal.ZERO : plan.getReceivedAmount();
        BigDecimal newReceived = received.add(payment.getAmount());
        plan.setReceivedAmount(newReceived);
        plan.setStatus(newReceived.compareTo(plan.getPlanAmount()) >= 0 ? 2 : 1);
        if (paymentPlanMapper.updateById(plan) == 0) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }

        payment.setPlanId(planId);
        payment.setWriteoffStatus(1);
        payment.setWriteoffTime(LocalDateTime.now());
        if (!updateById(payment)) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    public PaymentVO detail(Long id) {
        Payment entity = getById(id);
        if (entity == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "回款记录不存在");
        }
        return toVO(entity);
    }

    public PageResult<PaymentVO> pageQuery(PaymentQuery query) {
        Page<Payment> page = page(Page.of(query.getPageNum(), query.getPageSize()),
                Wrappers.<Payment>lambdaQuery()
                        .eq(query.getOrderId() != null, Payment::getOrderId, query.getOrderId())
                        .eq(query.getPlanId() != null, Payment::getPlanId, query.getPlanId())
                        .eq(query.getWriteoffStatus() != null,
                                Payment::getWriteoffStatus, query.getWriteoffStatus())
                        .orderByDesc(Payment::getId));
        return PageResult.of(page, this::toVO);
    }

    private void assertPlanInOrder(Long planId, Long orderId) {
        PaymentPlan plan = paymentPlanMapper.selectById(planId);
        if (plan == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "回款计划不存在");
        }
        if (!Objects.equals(plan.getOrderId(), orderId)) {
            throw new BizException("回款计划与订单不一致");
        }
    }

    private boolean isWrittenOff(Payment payment) {
        return payment.getWriteoffStatus() != null && payment.getWriteoffStatus() == 1;
    }

    private void applyForm(Payment entity, PaymentForm form) {
        entity.setPlanId(form.getPlanId());
        entity.setOrderId(form.getOrderId());
        entity.setAmount(form.getAmount());
        entity.setPaymentDate(form.getPaymentDate());
        entity.setRemark(form.getRemark());
    }

    private PaymentVO toVO(Payment entity) {
        PaymentVO vo = new PaymentVO();
        vo.setId(entity.getId());
        vo.setPlanId(entity.getPlanId());
        vo.setOrderId(entity.getOrderId());
        vo.setAmount(entity.getAmount());
        vo.setPaymentDate(entity.getPaymentDate());
        vo.setWriteoffStatus(entity.getWriteoffStatus());
        vo.setWriteoffStatusName(PaymentDicts.writeoffStatusName(entity.getWriteoffStatus()));
        vo.setWriteoffTime(entity.getWriteoffTime());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
