package cn.easykits.crm.modules.payment.service;

import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.ResultCode;
import cn.easykits.crm.common.exception.BizException;
import cn.easykits.crm.entity.Refund;
import cn.easykits.crm.mapper.RefundMapper;
import cn.easykits.crm.modules.payment.constant.PaymentDicts;
import cn.easykits.crm.modules.payment.dto.RefundForm;
import cn.easykits.crm.modules.payment.vo.RefundVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 退货 / 退款服务：CRUD / 分页 / 状态流转。refundNo 留空自动生成且唯一。
 */
@Service
@RequiredArgsConstructor
public class RefundServiceImpl extends ServiceImpl<RefundMapper, Refund> {

    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Transactional(rollbackFor = Exception.class)
    public Long createFromForm(RefundForm form) {
        Refund entity = new Refund();
        applyForm(entity, form);
        entity.setRefundNo(resolveNo(form.getRefundNo(), null));
        entity.setStatus(0);
        save(entity);
        return entity.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateFromForm(Long id, RefundForm form) {
        Refund entity = getById(id);
        if (entity == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "退款申请不存在");
        }
        if (isTerminal(entity.getStatus())) {
            throw new BizException("已退款或已驳回的申请不可修改");
        }
        applyForm(entity, form);
        entity.setRefundNo(resolveNo(form.getRefundNo(), id));
        if (!updateById(entity)) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long id, Integer status) {
        Refund entity = getById(id);
        if (entity == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "退款申请不存在");
        }
        if (isTerminal(entity.getStatus())) {
            throw new BizException("已退款或已驳回的申请不可再变更状态");
        }
        if (status == null || status < 0 || status > 3) {
            throw new BizException("目标状态不合法");
        }
        entity.setStatus(status);
        if (!updateById(entity)) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Refund entity = getById(id);
        if (entity == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "退款申请不存在");
        }
        removeById(id);
    }

    public RefundVO detail(Long id) {
        Refund entity = getById(id);
        if (entity == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "退款申请不存在");
        }
        return toVO(entity);
    }

    public PageResult<RefundVO> pageQuery(long pageNum, long pageSize,
                                          Long orderId, Long customerId, Integer status) {
        Page<Refund> page = page(Page.of(pageNum, pageSize),
                Wrappers.<Refund>lambdaQuery()
                        .eq(orderId != null, Refund::getOrderId, orderId)
                        .eq(customerId != null, Refund::getCustomerId, customerId)
                        .eq(status != null, Refund::getStatus, status)
                        .orderByDesc(Refund::getId));
        return PageResult.of(page, this::toVO);
    }

    private boolean isTerminal(Integer status) {
        return status != null && (status == 2 || status == 3);
    }

    private String resolveNo(String refundNo, Long excludeId) {
        if (!StringUtils.hasText(refundNo)) {
            return "RF-" + LocalDateTime.now().format(NO_FMT) + "-"
                    + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        }
        Refund same = getOne(Wrappers.<Refund>lambdaQuery()
                .eq(Refund::getRefundNo, refundNo)
                .ne(excludeId != null, Refund::getId, excludeId)
                .last("limit 1"));
        if (same != null) {
            throw new BizException("退款单号已存在: " + refundNo);
        }
        return refundNo;
    }

    private void applyForm(Refund entity, RefundForm form) {
        entity.setOrderId(form.getOrderId());
        entity.setCustomerId(form.getCustomerId());
        entity.setAmount(form.getAmount());
        entity.setReason(form.getReason());
        entity.setRefundDate(form.getRefundDate());
        entity.setRemark(form.getRemark());
    }

    private RefundVO toVO(Refund entity) {
        RefundVO vo = new RefundVO();
        vo.setId(entity.getId());
        vo.setRefundNo(entity.getRefundNo());
        vo.setOrderId(entity.getOrderId());
        vo.setCustomerId(entity.getCustomerId());
        vo.setAmount(entity.getAmount());
        vo.setReason(entity.getReason());
        vo.setStatus(entity.getStatus());
        vo.setStatusName(PaymentDicts.refundStatusName(entity.getStatus()));
        vo.setRefundDate(entity.getRefundDate());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
