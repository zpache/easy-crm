package cn.easykits.crm.modules.payment.service;

import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.ResultCode;
import cn.easykits.crm.common.exception.BizException;
import cn.easykits.crm.entity.Invoice;
import cn.easykits.crm.mapper.InvoiceMapper;
import cn.easykits.crm.modules.payment.constant.PaymentDicts;
import cn.easykits.crm.modules.payment.dto.InvoiceForm;
import cn.easykits.crm.modules.payment.vo.InvoiceVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 开票申请服务：CRUD / 分页 / 状态流转。终态（已开票/已作废）不可编辑。
 */
@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl extends ServiceImpl<InvoiceMapper, Invoice> {

    @Transactional(rollbackFor = Exception.class)
    public Long createFromForm(InvoiceForm form) {
        Invoice entity = new Invoice();
        applyForm(entity, form);
        entity.setStatus(0);
        save(entity);
        return entity.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateFromForm(Long id, InvoiceForm form) {
        Invoice entity = getById(id);
        if (entity == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "开票申请不存在");
        }
        if (isTerminal(entity.getStatus())) {
            throw new BizException("已开票或已作废的申请不可修改");
        }
        applyForm(entity, form);
        if (!updateById(entity)) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long id, Integer status) {
        Invoice entity = getById(id);
        if (entity == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "开票申请不存在");
        }
        if (isTerminal(entity.getStatus())) {
            throw new BizException("已开票或已作废的申请不可再变更状态");
        }
        if (status == null || status < 0 || status > 2) {
            throw new BizException("目标状态不合法");
        }
        entity.setStatus(status);
        if (!updateById(entity)) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Invoice entity = getById(id);
        if (entity == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "开票申请不存在");
        }
        removeById(id);
    }

    public InvoiceVO detail(Long id) {
        Invoice entity = getById(id);
        if (entity == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "开票申请不存在");
        }
        return toVO(entity);
    }

    public PageResult<InvoiceVO> pageQuery(long pageNum, long pageSize,
                                           Long orderId, Long customerId, Integer status) {
        Page<Invoice> page = page(Page.of(pageNum, pageSize),
                Wrappers.<Invoice>lambdaQuery()
                        .eq(orderId != null, Invoice::getOrderId, orderId)
                        .eq(customerId != null, Invoice::getCustomerId, customerId)
                        .eq(status != null, Invoice::getStatus, status)
                        .orderByDesc(Invoice::getId));
        return PageResult.of(page, this::toVO);
    }

    private boolean isTerminal(Integer status) {
        return status != null && (status == 1 || status == 2);
    }

    private void applyForm(Invoice entity, InvoiceForm form) {
        entity.setInvoiceNo(form.getInvoiceNo());
        entity.setOrderId(form.getOrderId());
        entity.setCustomerId(form.getCustomerId());
        entity.setAmount(form.getAmount());
        entity.setTitle(form.getTitle());
        entity.setTaxNo(form.getTaxNo());
        entity.setApplyDate(form.getApplyDate());
        entity.setRemark(form.getRemark());
    }

    private InvoiceVO toVO(Invoice entity) {
        InvoiceVO vo = new InvoiceVO();
        vo.setId(entity.getId());
        vo.setInvoiceNo(entity.getInvoiceNo());
        vo.setOrderId(entity.getOrderId());
        vo.setCustomerId(entity.getCustomerId());
        vo.setAmount(entity.getAmount());
        vo.setTitle(entity.getTitle());
        vo.setTaxNo(entity.getTaxNo());
        vo.setStatus(entity.getStatus());
        vo.setStatusName(PaymentDicts.invoiceStatusName(entity.getStatus()));
        vo.setApplyDate(entity.getApplyDate());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
