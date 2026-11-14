package cn.easykits.crm.modules.product.service;

import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.ResultCode;
import cn.easykits.crm.common.context.LoginUser;
import cn.easykits.crm.common.context.LoginUserContext;
import cn.easykits.crm.common.exception.BizException;
import cn.easykits.crm.entity.Customer;
import cn.easykits.crm.entity.Quote;
import cn.easykits.crm.entity.QuoteItem;
import cn.easykits.crm.mapper.CustomerMapper;
import cn.easykits.crm.mapper.QuoteItemMapper;
import cn.easykits.crm.mapper.QuoteMapper;
import cn.easykits.crm.modules.product.constant.ProductDicts;
import cn.easykits.crm.modules.product.dto.QuoteForm;
import cn.easykits.crm.modules.product.dto.QuoteItemForm;
import cn.easykits.crm.modules.product.dto.QuoteQuery;
import cn.easykits.crm.modules.product.vo.QuoteItemVO;
import cn.easykits.crm.modules.product.vo.QuoteVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 报价单服务：主表金额与明细汇总强一致。
 * <p>校验规则（创建/更新均执行）：Σ(单价*数量-折扣) 必须等于 totalAmount，
 * 否则抛 BizException 拒绝保存；明细行不允许为空。
 */
@Service
@RequiredArgsConstructor
public class QuoteServiceImpl extends ServiceImpl<QuoteMapper, Quote> {

    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final QuoteItemMapper quoteItemMapper;
    private final CustomerMapper customerMapper;

    @Transactional(rollbackFor = Exception.class)
    public Long createFromForm(QuoteForm form) {
        BigDecimal detailSum = assertAndSumItems(form);
        if (detailSum.compareTo(form.getTotalAmount()) != 0) {
            throw new BizException("明细汇总与总金额不一致，拒绝保存");
        }
        String quoteNo = resolveQuoteNo(form.getQuoteNo(), null);
        LoginUser user = LoginUserContext.require();

        Quote quote = new Quote();
        quote.setQuoteNo(quoteNo);
        quote.setOpportunityId(form.getOpportunityId());
        quote.setCustomerId(form.getCustomerId());
        quote.setTotalAmount(form.getTotalAmount());
        quote.setStatus(form.getStatus() == null ? 0 : form.getStatus());
        quote.setValidUntil(form.getValidUntil());
        quote.setOwnerId(user.userId());
        quote.setDeptId(user.deptId());
        quote.setRemark(form.getRemark());
        save(quote);

        saveItems(quote.getId(), form.getItems());
        return quote.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateFromForm(Long id, QuoteForm form) {
        Quote quote = getById(id);
        if (quote == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "报价单不存在");
        }
        BigDecimal detailSum = assertAndSumItems(form);   // 校验结果显式比对，见下方
        if (detailSum.compareTo(form.getTotalAmount()) != 0) {
            throw new BizException("明细汇总与总金额不一致，拒绝保存");
        }
        String quoteNo = resolveQuoteNo(form.getQuoteNo(), id);

        quote.setQuoteNo(quoteNo);
        quote.setOpportunityId(form.getOpportunityId());
        quote.setCustomerId(form.getCustomerId());
        quote.setTotalAmount(form.getTotalAmount());
        quote.setStatus(form.getStatus() == null ? quote.getStatus() : form.getStatus());
        quote.setValidUntil(form.getValidUntil());
        quote.setRemark(form.getRemark());
        if (!updateById(quote)) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }

        // 明细全量替换：先逻辑删除旧行，再按表单重建（快照不复用旧主键）
        quoteItemMapper.delete(Wrappers.<QuoteItem>lambdaQuery()
                .eq(QuoteItem::getQuoteId, id));
        saveItems(id, form.getItems());
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Quote quote = getById(id);
        if (quote == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "报价单不存在");
        }
        quoteItemMapper.delete(Wrappers.<QuoteItem>lambdaQuery()
                .eq(QuoteItem::getQuoteId, id));
        removeById(id);
    }

    public QuoteVO detail(Long id) {
        Quote quote = getById(id);
        if (quote == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "报价单不存在");
        }
        QuoteVO vo = toVO(quote);
        fillCustomerNames(List.of(vo));
        vo.setItems(quoteItemMapper.selectList(Wrappers.<QuoteItem>lambdaQuery()
                        .eq(QuoteItem::getQuoteId, id)
                        .orderByAsc(QuoteItem::getId))
                .stream().map(this::toItemVO).toList());
        return vo;
    }

    public PageResult<QuoteVO> pageQuery(QuoteQuery query) {
        Page<Quote> page = page(Page.of(query.getPageNum(), query.getPageSize()),
                Wrappers.<Quote>lambdaQuery()
                        .eq(query.getCustomerId() != null, Quote::getCustomerId, query.getCustomerId())
                        .eq(query.getOpportunityId() != null, Quote::getOpportunityId, query.getOpportunityId())
                        .eq(query.getStatus() != null, Quote::getStatus, query.getStatus())
                        .like(StringUtils.hasText(query.getQuoteNo()), Quote::getQuoteNo, query.getQuoteNo())
                        .orderByDesc(Quote::getId));
        PageResult<QuoteVO> result = PageResult.of(page, this::toVO);
        fillCustomerNames(result.records());
        return result;
    }

    /** 校验每行并计算明细汇总；同时把校验过的 subtotal 记回表单供 saveItems 复用。 */
    private BigDecimal assertAndSumItems(QuoteForm form) {
        if (form.getItems() == null || form.getItems().isEmpty()) {
            throw new BizException("报价明细不能为空");
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (QuoteItemForm item : form.getItems()) {
            BigDecimal discount = item.getDiscount() == null ? BigDecimal.ZERO : item.getDiscount();
            BigDecimal line = item.getUnitPrice().multiply(item.getQuantity()).subtract(discount);
            if (line.signum() < 0) {
                throw new BizException("明细「%s」小计不能为负数".formatted(item.getProductName()));
            }
            sum = sum.add(line);
        }
        return sum;
    }

    private void saveItems(Long quoteId, List<QuoteItemForm> items) {
        for (QuoteItemForm item : items) {
            BigDecimal discount = item.getDiscount() == null ? BigDecimal.ZERO : item.getDiscount();
            QuoteItem entity = new QuoteItem();
            entity.setQuoteId(quoteId);
            entity.setProductId(item.getProductId());
            entity.setProductName(item.getProductName());
            entity.setUnitPrice(item.getUnitPrice());
            entity.setQuantity(item.getQuantity());
            entity.setDiscount(discount);
            entity.setSubtotal(item.getUnitPrice().multiply(item.getQuantity()).subtract(discount));
            entity.setRemark(item.getRemark());
            quoteItemMapper.insert(entity);
        }
    }

    /** quoteNo 留空自动生成；显式指定时校验唯一（更新时排除自身）。 */
    private String resolveQuoteNo(String quoteNo, Long excludeId) {
        if (!StringUtils.hasText(quoteNo)) {
            return "QT-" + LocalDateTime.now().format(NO_FMT) + "-"
                    + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        }
        Quote same = getOne(Wrappers.<Quote>lambdaQuery()
                .eq(Quote::getQuoteNo, quoteNo)
                .ne(excludeId != null, Quote::getId, excludeId)
                .last("limit 1"));
        if (same != null) {
            throw new BizException("报价单编号已存在: " + quoteNo);
        }
        return quoteNo;
    }

    private void fillCustomerNames(List<QuoteVO> vos) {
        List<Long> ids = vos.stream().map(QuoteVO::getCustomerId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, String> nameMap = ids.isEmpty() ? Map.of()
                : customerMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Customer::getId, Customer::getName));
        for (QuoteVO vo : vos) {
            vo.setCustomerName(nameMap.get(vo.getCustomerId()));
        }
    }

    private QuoteVO toVO(Quote quote) {
        QuoteVO vo = new QuoteVO();
        vo.setId(quote.getId());
        vo.setQuoteNo(quote.getQuoteNo());
        vo.setOpportunityId(quote.getOpportunityId());
        vo.setCustomerId(quote.getCustomerId());
        vo.setTotalAmount(quote.getTotalAmount());
        vo.setStatus(quote.getStatus());
        vo.setStatusName(ProductDicts.quoteStatusName(quote.getStatus()));
        vo.setValidUntil(quote.getValidUntil());
        vo.setOwnerId(quote.getOwnerId());
        vo.setDeptId(quote.getDeptId());
        vo.setRemark(quote.getRemark());
        vo.setCreateTime(quote.getCreateTime());
        vo.setUpdateTime(quote.getUpdateTime());
        return vo;
    }

    private QuoteItemVO toItemVO(QuoteItem entity) {
        QuoteItemVO vo = new QuoteItemVO();
        vo.setId(entity.getId());
        vo.setQuoteId(entity.getQuoteId());
        vo.setProductId(entity.getProductId());
        vo.setProductName(entity.getProductName());
        vo.setUnitPrice(entity.getUnitPrice());
        vo.setQuantity(entity.getQuantity());
        vo.setDiscount(entity.getDiscount());
        vo.setSubtotal(entity.getSubtotal());
        vo.setRemark(entity.getRemark());
        return vo;
    }
}
