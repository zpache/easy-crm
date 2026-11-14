package cn.easykits.crm.modules.product.service;

import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.ResultCode;
import cn.easykits.crm.common.exception.BizException;
import cn.easykits.crm.entity.OpportunityItem;
import cn.easykits.crm.mapper.OpportunityItemMapper;
import cn.easykits.crm.modules.product.dto.OpportunityItemForm;
import cn.easykits.crm.modules.product.vo.OpportunityItemVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商机产品明细服务。subtotal = 单价 * 数量 - 折扣，全部 BigDecimal，服务端计算。
 */
@Service
@RequiredArgsConstructor
public class OpportunityItemServiceImpl extends ServiceImpl<OpportunityItemMapper, OpportunityItem> {

    @Transactional(rollbackFor = Exception.class)
    public Long createFromForm(OpportunityItemForm form) {
        OpportunityItem entity = new OpportunityItem();
        applyForm(entity, form);
        entity.setSubtotal(subtotal(form.getUnitPrice(), form.getQuantity(), form.getDiscount()));
        save(entity);
        return entity.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateFromForm(Long id, OpportunityItemForm form) {
        OpportunityItem entity = getById(id);
        if (entity == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "商机产品明细不存在");
        }
        applyForm(entity, form);
        entity.setSubtotal(subtotal(form.getUnitPrice(), form.getQuantity(), form.getDiscount()));
        if (!updateById(entity)) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        OpportunityItem entity = getById(id);
        if (entity == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "商机产品明细不存在");
        }
        removeById(id);
    }

    public OpportunityItemVO detail(Long id) {
        OpportunityItem entity = getById(id);
        if (entity == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "商机产品明细不存在");
        }
        return toVO(entity);
    }

    public PageResult<OpportunityItemVO> pageQuery(long pageNum, long pageSize, Long opportunityId) {
        Page<OpportunityItem> page = page(Page.of(pageNum, pageSize),
                Wrappers.<OpportunityItem>lambdaQuery()
                        .eq(opportunityId != null, OpportunityItem::getOpportunityId, opportunityId)
                        .orderByAsc(OpportunityItem::getId));
        return PageResult.of(page, this::toVO);
    }

    /** 某商机下全部有效明细的小计之和。 */
    public BigDecimal summary(Long opportunityId) {
        List<OpportunityItem> items = list(Wrappers.<OpportunityItem>lambdaQuery()
                .eq(OpportunityItem::getOpportunityId, opportunityId));
        return items.stream()
                .map(OpportunityItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** 小计 = 单价 * 数量 - 折扣；折扣为空按 0，结果不允许为负。 */
    private BigDecimal subtotal(BigDecimal unitPrice, BigDecimal quantity, BigDecimal discount) {
        BigDecimal discountValue = discount == null ? BigDecimal.ZERO : discount;
        BigDecimal value = unitPrice.multiply(quantity).subtract(discountValue);
        if (value.signum() < 0) {
            throw new BizException("明细小计不能为负数（折扣不可超过单价×数量）");
        }
        return value;
    }

    private void applyForm(OpportunityItem entity, OpportunityItemForm form) {
        entity.setOpportunityId(form.getOpportunityId());
        entity.setProductId(form.getProductId());
        entity.setProductName(form.getProductName());
        entity.setUnitPrice(form.getUnitPrice());
        entity.setQuantity(form.getQuantity());
        entity.setDiscount(form.getDiscount() == null ? BigDecimal.ZERO : form.getDiscount());
        entity.setRemark(form.getRemark());
    }

    private OpportunityItemVO toVO(OpportunityItem entity) {
        OpportunityItemVO vo = new OpportunityItemVO();
        vo.setId(entity.getId());
        vo.setOpportunityId(entity.getOpportunityId());
        vo.setProductId(entity.getProductId());
        vo.setProductName(entity.getProductName());
        vo.setUnitPrice(entity.getUnitPrice());
        vo.setQuantity(entity.getQuantity());
        vo.setDiscount(entity.getDiscount());
        vo.setSubtotal(entity.getSubtotal());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
