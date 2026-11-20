package cn.easykits.crm.modules.order.service;

import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.ResultCode;
import cn.easykits.crm.common.context.LoginUser;
import cn.easykits.crm.common.context.LoginUserContext;
import cn.easykits.crm.common.exception.BizException;
import cn.easykits.crm.entity.Customer;
import cn.easykits.crm.entity.Opportunity;
import cn.easykits.crm.entity.Order;
import cn.easykits.crm.entity.OrderItem;
import cn.easykits.crm.entity.PaymentPlan;
import cn.easykits.crm.mapper.CustomerMapper;
import cn.easykits.crm.mapper.OpportunityMapper;
import cn.easykits.crm.mapper.OrderItemMapper;
import cn.easykits.crm.mapper.OrderMapper;
import cn.easykits.crm.mapper.PaymentPlanMapper;
import cn.easykits.crm.modules.opportunity.event.OpportunityWonEvent;
import cn.easykits.crm.modules.order.constant.OrderStatus;
import cn.easykits.crm.modules.order.dto.OrderForm;
import cn.easykits.crm.modules.order.dto.OrderItemForm;
import cn.easykits.crm.modules.order.dto.OrderQuery;
import cn.easykits.crm.modules.order.dto.OrderStatusForm;
import cn.easykits.crm.modules.order.vo.OrderItemVO;
import cn.easykits.crm.modules.order.vo.OrderVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 订单服务：状态机、主子表金额强一致、赢单联动建单入口。
 * <p>明细汇总 Σ(单价*数量-折扣) 必须等于 totalAmount，不一致拒绝保存。
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> {

    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OrderItemMapper orderItemMapper;
    private final PaymentPlanMapper paymentPlanMapper;
    private final CustomerMapper customerMapper;
    private final OpportunityMapper opportunityMapper;
    private final cn.easykits.crm.mapper.QuoteMapper quoteMapper;

    // ============================================================ 管理端 CRUD

    @Transactional(rollbackFor = Exception.class)
    public Long createFromForm(OrderForm form) {
        BigDecimal detailSum = assertAndSumItems(form);
        if (detailSum.compareTo(form.getTotalAmount()) != 0) {
            throw new BizException("明细汇总与总金额不一致，拒绝保存");
        }
        LoginUser user = LoginUserContext.require();
        Order order = new Order();
        applyForm(order, form);
        order.setOrderNo(resolveNo(form.getOrderNo(), null));
        order.setStatus(OrderStatus.DRAFT);
        order.setOwnerId(user.userId());
        order.setDeptId(user.deptId());
        save(order);
        saveItems(order.getId(), form.getItems());
        return order.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateFromForm(Long id, OrderForm form) {
        Order order = getById(id);
        if (order == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "订单不存在");
        }
        if (OrderStatus.isTerminal(statusOf(order))) {
            throw new BizException("订单已处于终态，不可编辑");
        }
        BigDecimal detailSum = assertAndSumItems(form);
        if (detailSum.compareTo(form.getTotalAmount()) != 0) {
            throw new BizException("明细汇总与总金额不一致，拒绝保存");
        }
        applyForm(order, form);
        order.setOrderNo(resolveNo(form.getOrderNo(), id));
        if (!updateById(order)) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
        orderItemMapper.delete(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderId, id));
        saveItems(id, form.getItems());
    }

    /** 状态流转：由 OrderStatus 状态机统一判定，终态不可变更。 */
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long id, OrderStatusForm form) {
        Order order = getById(id);
        if (order == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "订单不存在");
        }
        int from = statusOf(order);
        Integer target = form.getStatus();
        if (target == null || !OrderStatus.canTransition(from, target)) {
            throw new BizException("不允许的状态流转：%s -> %s"
                    .formatted(OrderStatus.name(from), OrderStatus.name(target)));
        }
        order.setStatus(target);
        if (form.getShipDate() != null) {
            order.setShipDate(form.getShipDate());
        }
        if (form.getDeliveryDate() != null) {
            order.setDeliveryDate(form.getDeliveryDate());
        }
        if (!updateById(order)) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Order order = getById(id);
        if (order == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "订单不存在");
        }
        if (OrderStatus.isTerminal(statusOf(order))) {
            throw new BizException("订单已处于终态，不可删除");
        }
        orderItemMapper.delete(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderId, id));
        removeById(id);
    }

    public OrderVO detail(Long id) {
        Order order = getById(id);
        if (order == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "订单不存在");
        }
        OrderVO vo = toVO(order);
        fillCustomerNames(List.of(vo));
        vo.setItems(items(id));
        return vo;
    }

    public List<OrderItemVO> items(Long orderId) {
        return orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                        .eq(OrderItem::getOrderId, orderId)
                        .orderByAsc(OrderItem::getId))
                .stream().map(this::toItemVO).toList();
    }

    public PageResult<OrderVO> pageQuery(OrderQuery query) {
        Page<Order> page = page(Page.of(query.getPageNum(), query.getPageSize()),
                Wrappers.<Order>lambdaQuery()
                        .eq(query.getCustomerId() != null, Order::getCustomerId, query.getCustomerId())
                        .eq(query.getOpportunityId() != null, Order::getOpportunityId, query.getOpportunityId())
                        .eq(query.getContractId() != null, Order::getContractId, query.getContractId())
                        .eq(query.getQuoteId() != null, Order::getQuoteId, query.getQuoteId())
                        .eq(query.getStatus() != null, Order::getStatus, query.getStatus())
                        .like(StringUtils.hasText(query.getOrderNo()), Order::getOrderNo, query.getOrderNo())
                        .orderByDesc(Order::getId));
        PageResult<OrderVO> result = PageResult.of(page, this::toVO);
        fillCustomerNames(result.records());
        return result;
    }

    // ============================================================ 赢单联动（供 AFTER_COMMIT 监听器调用）

    /**
     * 商机赢单后自动创建：订单草稿 + 一行金额快照明细 + 首期回款计划。
     * <p>幂等：同一商机已存在订单则直接返回其 ID，不重复建单。
     * <p>注意：本方法由 @Async AFTER_COMMIT 监听器调用，运行线程无登录态，
     * 因此绝不调用 LoginUserContext.require()，owner/dept 全部取自事件与商机主数据。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createDraftFromWon(OpportunityWonEvent event) {
        Long opportunityId = event.opportunityId();
        if (opportunityId != null) {
            Order existing = getOne(Wrappers.<Order>lambdaQuery()
                    .eq(Order::getOpportunityId, opportunityId)
                    .last("limit 1"));
            if (existing != null) {
                return existing.getId();
            }
        }
        BigDecimal amount = event.actualAmount() == null ? BigDecimal.ZERO : event.actualAmount();
        Long deptId = null;
        if (opportunityId != null) {
            Opportunity opportunity = opportunityMapper.selectById(opportunityId);
            if (opportunity != null) {
                deptId = opportunity.getDeptId();
            }
        }

        Order order = new Order();
        order.setOrderNo(generateNo("SO"));
        order.setCustomerId(event.customerId());
        order.setOpportunityId(opportunityId);
        order.setTotalAmount(amount);
        order.setStatus(OrderStatus.DRAFT);
        order.setOwnerId(event.ownerId());
        order.setDeptId(deptId);
        order.setRemark("商机赢单自动创建：" + event.name());
        save(order);

        // 单行快照明细，保证 Σ subtotal == totalAmount
        OrderItem item = new OrderItem();
        item.setOrderId(order.getId());
        item.setProductName(event.name());
        item.setUnitPrice(amount);
        item.setQuantity(BigDecimal.ONE);
        item.setDiscount(BigDecimal.ZERO);
        item.setSubtotal(amount);
        orderItemMapper.insert(item);

        // 首期回款计划
        PaymentPlan plan = new PaymentPlan();
        plan.setOrderId(order.getId());
        plan.setPeriodNo(1);
        plan.setPlanDate(event.actualCloseDate() == null
                ? LocalDate.now().plusDays(30) : event.actualCloseDate());
        plan.setPlanAmount(amount);
        plan.setReceivedAmount(BigDecimal.ZERO);
        plan.setStatus(0);
        plan.setRemark("赢单联动首期回款计划");
        paymentPlanMapper.insert(plan);

        return order.getId();
    }

    // ============================================================ 私有工具

    private int statusOf(Order order) {
        return order.getStatus() == null ? OrderStatus.DRAFT : order.getStatus();
    }

    /** 校验明细并计算汇总。 */
    private BigDecimal assertAndSumItems(OrderForm form) {
        if (form.getItems() == null || form.getItems().isEmpty()) {
            throw new BizException("订单明细不能为空");
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (OrderItemForm item : form.getItems()) {
            BigDecimal discount = item.getDiscount() == null ? BigDecimal.ZERO : item.getDiscount();
            BigDecimal line = item.getUnitPrice().multiply(item.getQuantity()).subtract(discount);
            if (line.signum() < 0) {
                throw new BizException("明细「%s」小计不能为负数".formatted(item.getProductName()));
            }
            sum = sum.add(line);
        }
        return sum;
    }

    private void saveItems(Long orderId, List<OrderItemForm> items) {
        for (OrderItemForm item : items) {
            BigDecimal discount = item.getDiscount() == null ? BigDecimal.ZERO : item.getDiscount();
            OrderItem entity = new OrderItem();
            entity.setOrderId(orderId);
            entity.setProductId(item.getProductId());
            entity.setProductName(item.getProductName());
            entity.setUnitPrice(item.getUnitPrice());
            entity.setQuantity(item.getQuantity());
            entity.setDiscount(discount);
            entity.setSubtotal(item.getUnitPrice().multiply(item.getQuantity()).subtract(discount));
            entity.setRemark(item.getRemark());
            orderItemMapper.insert(entity);
        }
    }

    private String resolveNo(String orderNo, Long excludeId) {
        if (!StringUtils.hasText(orderNo)) {
            return generateNo("SO");
        }
        Order same = getOne(Wrappers.<Order>lambdaQuery()
                .eq(Order::getOrderNo, orderNo)
                .ne(excludeId != null, Order::getId, excludeId)
                .last("limit 1"));
        if (same != null) {
            throw new BizException("订单编号已存在: " + orderNo);
        }
        return orderNo;
    }

    private String generateNo(String prefix) {
        return prefix + "-" + LocalDateTime.now().format(NO_FMT) + "-"
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    private void applyForm(Order order, OrderForm form) {
        order.setCustomerId(form.getCustomerId());
        order.setOpportunityId(form.getOpportunityId());
        order.setContractId(form.getContractId());
        // 报价单关联：校验存在性与客户归属（报价必须属于同一客户）
        if (form.getQuoteId() != null) {
            cn.easykits.crm.entity.Quote quote = quoteMapper.selectById(form.getQuoteId());
            if (quote == null) {
                throw new BizException(ResultCode.DATA_NOT_FOUND, "报价单不存在");
            }
            if (!quote.getCustomerId().equals(form.getCustomerId())) {
                throw new BizException("报价单「%s」不属于当前客户".formatted(quote.getQuoteNo()));
            }
            order.setQuoteId(form.getQuoteId());
        }
        order.setTotalAmount(form.getTotalAmount());
        order.setShipDate(form.getShipDate());
        order.setDeliveryDate(form.getDeliveryDate());
        order.setRemark(form.getRemark());
    }

    private void fillCustomerNames(List<OrderVO> vos) {
        List<Long> ids = vos.stream().map(OrderVO::getCustomerId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, String> nameMap = ids.isEmpty() ? Map.of()
                : customerMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Customer::getId, Customer::getName));
        for (OrderVO vo : vos) {
            vo.setCustomerName(nameMap.get(vo.getCustomerId()));
        }
    }

    private OrderVO toVO(Order order) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setCustomerId(order.getCustomerId());
        vo.setOpportunityId(order.getOpportunityId());
        vo.setContractId(order.getContractId());
        vo.setQuoteId(order.getQuoteId());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(order.getStatus());
        vo.setStatusName(OrderStatus.name(order.getStatus()));
        vo.setShipDate(order.getShipDate());
        vo.setDeliveryDate(order.getDeliveryDate());
        vo.setOwnerId(order.getOwnerId());
        vo.setDeptId(order.getDeptId());
        vo.setRemark(order.getRemark());
        vo.setCreateTime(order.getCreateTime());
        vo.setUpdateTime(order.getUpdateTime());
        return vo;
    }

    private OrderItemVO toItemVO(OrderItem entity) {
        OrderItemVO vo = new OrderItemVO();
        vo.setId(entity.getId());
        vo.setOrderId(entity.getOrderId());
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
