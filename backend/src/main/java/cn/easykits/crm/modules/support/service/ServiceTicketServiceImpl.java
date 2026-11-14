package cn.easykits.crm.modules.support.service;

import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.ResultCode;
import cn.easykits.crm.common.context.LoginUser;
import cn.easykits.crm.common.context.LoginUserContext;
import cn.easykits.crm.common.exception.BizException;
import cn.easykits.crm.entity.Contact;
import cn.easykits.crm.entity.Customer;
import cn.easykits.crm.entity.Reminder;
import cn.easykits.crm.entity.ServiceTicket;
import cn.easykits.crm.entity.ServiceTicketLog;
import cn.easykits.crm.entity.SlaPolicy;
import cn.easykits.crm.entity.SysUser;
import cn.easykits.crm.mapper.ContactMapper;
import cn.easykits.crm.mapper.CustomerMapper;
import cn.easykits.crm.mapper.ReminderMapper;
import cn.easykits.crm.mapper.ServiceTicketLogMapper;
import cn.easykits.crm.mapper.ServiceTicketMapper;
import cn.easykits.crm.mapper.SlaPolicyMapper;
import cn.easykits.crm.mapper.SysUserMapper;
import cn.easykits.crm.modules.support.constant.SupportDicts;
import cn.easykits.crm.modules.support.constant.TicketStatus;
import cn.easykits.crm.modules.support.dto.ServiceTicketCreateForm;
import cn.easykits.crm.modules.support.dto.ServiceTicketQuery;
import cn.easykits.crm.modules.support.dto.ServiceTicketUpdateForm;
import cn.easykits.crm.modules.support.dto.TicketAssignForm;
import cn.easykits.crm.modules.support.dto.TicketCloseForm;
import cn.easykits.crm.modules.support.dto.TicketStatusChangeForm;
import cn.easykits.crm.modules.support.vo.ServiceTicketLogVO;
import cn.easykits.crm.modules.support.vo.ServiceTicketVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 服务工单服务：CRUD / 状态流转 / 派单 / 关闭 / SLA 计算 / 流转日志 / 超时扫描。
 *
 * <p>状态机合法性（含终态不可再变）在 Service 层强制校验，见 {@link TicketStatus}；
 * 每次状态变更与流转日志在同一事务写入（{@link #writeLog}）。
 *
 * <p>行级数据权限：crm_service_ticket 需登记到 DataScopeTableRegistry（owner_id + dept_id）。
 * 超时扫描 {@link #scanOverdueTickets()} 由 {@code ServiceSlaTask} 在
 * {@code DataScopeContext.runAsSystem(...)} 的系统上下文里调用，不读取登录用户。
 */
@Service
@RequiredArgsConstructor
public class ServiceTicketServiceImpl extends ServiceImpl<ServiceTicketMapper, ServiceTicket> {

    private final SlaPolicyMapper slaPolicyMapper;
    private final ServiceTicketLogMapper ticketLogMapper;
    private final CustomerMapper customerMapper;
    private final ContactMapper contactMapper;
    private final SysUserMapper sysUserMapper;
    private final ReminderMapper reminderMapper;

    // ============================================================ 创建 / 编辑

    /**
     * 创建工单：初始状态 PENDING（待受理），按 (ticketType, priority) 匹配启用 SLA 策略
     * 计算应响应 / 应解决时间；无匹配策略时取兜底时限。创建日志同事务写入。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(ServiceTicketCreateForm form) {
        if (!SupportDicts.isValidType(form.getTicketType())) {
            throw new BizException("工单类型不合法: " + form.getTicketType());
        }
        if (!SupportDicts.isValidPriority(form.getPriority())) {
            throw new BizException("优先级不合法: " + form.getPriority());
        }
        if (StringUtils.hasText(form.getSource()) && !SupportDicts.isValidSource(form.getSource())) {
            throw new BizException("来源不合法: " + form.getSource());
        }
        Customer customer = customerMapper.selectById(form.getCustomerId());
        if (customer == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "所属客户不存在");
        }
        if (form.getContactId() != null) {
            assertContactInCustomer(form.getContactId(), form.getCustomerId());
        }

        LoginUser user = LoginUserContext.require();
        LocalDateTime now = LocalDateTime.now();
        SlaPolicy policy = resolveSlaPolicy(form.getTicketType(), form.getPriority());
        int responseHours = policy == null
                ? SupportDicts.DEFAULT_RESPONSE_HOURS : policy.getResponseHours();
        int resolveHours = policy == null
                ? SupportDicts.DEFAULT_RESOLVE_HOURS : policy.getResolveHours();

        ServiceTicket ticket = new ServiceTicket();
        ticket.setTicketNo(generateTicketNo());
        ticket.setTitle(form.getTitle());
        ticket.setCustomerId(form.getCustomerId());
        ticket.setContactId(form.getContactId());
        ticket.setOrderId(form.getOrderId());
        ticket.setTicketType(form.getTicketType());
        ticket.setPriority(form.getPriority());
        ticket.setStatus(TicketStatus.PENDING.code());
        // 录入人作为初始负责人，保证 SELF 数据范围下可见；派单后覆盖为受理人
        ticket.setOwnerId(user.userId());
        ticket.setDeptId(user.deptId());
        ticket.setSlaDueResponseTime(now.plusHours(responseHours));
        ticket.setSlaDueResolveTime(now.plusHours(resolveHours));
        ticket.setActualResponseTime(null);
        ticket.setActualResolveTime(null);
        ticket.setOverdueFlag(0);
        ticket.setSource(StringUtils.hasText(form.getSource()) ? form.getSource() : null);
        ticket.setDescription(form.getDescription());
        save(ticket);

        writeLog(ticket.getId(), null, TicketStatus.PENDING, "创建工单");
        return ticket.getId();
    }

    /**
     * 编辑工单：终态不可编辑；customerId / ticketType / priority 不可变更（避免 SLA 时间漂移）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ServiceTicketUpdateForm form) {
        ServiceTicket ticket = requireTicket(id);
        assertNotTerminal(ticket);
        if (StringUtils.hasText(form.getTitle())) {
            ticket.setTitle(form.getTitle());
        }
        if (form.getContactId() != null) {
            assertContactInCustomer(form.getContactId(), ticket.getCustomerId());
            ticket.setContactId(form.getContactId());
        }
        ticket.setOrderId(form.getOrderId() != null ? form.getOrderId() : ticket.getOrderId());
        if (form.getSource() != null) {
            if (StringUtils.hasText(form.getSource()) && !SupportDicts.isValidSource(form.getSource())) {
                throw new BizException("来源不合法: " + form.getSource());
            }
            ticket.setSource(StringUtils.hasText(form.getSource()) ? form.getSource() : null);
        }
        ticket.setDescription(form.getDescription() != null ? form.getDescription() : ticket.getDescription());
        if (!updateById(ticket)) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    // ============================================================ 查询

    /** 分页查询（关键字 / 状态 / 类型 / 优先级 / 客户 / 受理人 / 超时标记 / 创建时间范围）。 */
    public PageResult<ServiceTicketVO> pageQuery(ServiceTicketQuery query) {
        Page<ServiceTicket> page = page(Page.of(query.getPageNum(), query.getPageSize()),
                Wrappers.<ServiceTicket>lambdaQuery()
                        .and(StringUtils.hasText(query.getKeyword()), w -> w
                                .like(ServiceTicket::getTicketNo, query.getKeyword())
                                .or().like(ServiceTicket::getTitle, query.getKeyword()))
                        .eq(StringUtils.hasText(query.getStatus()), ServiceTicket::getStatus, query.getStatus())
                        .eq(StringUtils.hasText(query.getTicketType()), ServiceTicket::getTicketType, query.getTicketType())
                        .eq(StringUtils.hasText(query.getPriority()), ServiceTicket::getPriority, query.getPriority())
                        .eq(query.getCustomerId() != null, ServiceTicket::getCustomerId, query.getCustomerId())
                        .isNull(query.getOwnerId() != null && query.getOwnerId() == -1, ServiceTicket::getOwnerId)
                        .eq(query.getOwnerId() != null && query.getOwnerId() != -1,
                                ServiceTicket::getOwnerId, query.getOwnerId())
                        .eq(query.getOverdueFlag() != null, ServiceTicket::getOverdueFlag, query.getOverdueFlag())
                        .eq(StringUtils.hasText(query.getSource()), ServiceTicket::getSource, query.getSource())
                        .ge(query.getCreateTimeStart() != null, ServiceTicket::getCreateTime, query.getCreateTimeStart())
                        .le(query.getCreateTimeEnd() != null, ServiceTicket::getCreateTime, query.getCreateTimeEnd())
                        .orderByDesc(ServiceTicket::getId));
        List<ServiceTicketVO> vos = page.getRecords().stream().map(this::toVO).toList();
        fillNames(vos, page.getRecords());
        return new PageResult<>(vos, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    /** 详情。 */
    public ServiceTicketVO detail(Long id) {
        ServiceTicket ticket = requireTicket(id);
        ServiceTicketVO vo = toVO(ticket);
        fillNames(List.of(vo), List.of(ticket));
        return vo;
    }

    /** 流转日志（时间线）：按操作时间正序，批量补操作人名称与状态名。 */
    public List<ServiceTicketLogVO> logs(Long id) {
        List<ServiceTicketLog> records = ticketLogMapper.selectList(
                Wrappers.<ServiceTicketLog>lambdaQuery()
                        .eq(ServiceTicketLog::getTicketId, id)
                        .orderByAsc(ServiceTicketLog::getOperateTime)
                        .orderByAsc(ServiceTicketLog::getId));
        List<ServiceTicketLogVO> vos = records.stream().map(log -> {
            ServiceTicketLogVO vo = new ServiceTicketLogVO();
            vo.setId(log.getId());
            vo.setTicketId(log.getTicketId());
            vo.setFromStatus(log.getFromStatus());
            vo.setFromStatusName(TicketStatus.labelOf(log.getFromStatus()));
            vo.setToStatus(log.getToStatus());
            vo.setToStatusName(TicketStatus.labelOf(log.getToStatus()));
            vo.setOperatorId(log.getOperatorId());
            vo.setOperateTime(log.getOperateTime());
            vo.setRemark(log.getRemark());
            return vo;
        }).toList();
        List<Long> operatorIds = vos.stream().map(ServiceTicketLogVO::getOperatorId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, String> nameMap = operatorIds.isEmpty() ? Map.of()
                : sysUserMapper.selectBatchIds(operatorIds).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getNickname));
        for (ServiceTicketLogVO vo : vos) {
            vo.setOperatorName(vo.getOperatorId() == null ? null : nameMap.get(vo.getOperatorId()));
        }
        return vos;
    }

    // ============================================================ 状态流转 / 派单 / 关闭

    /**
     * 状态流转：目标状态必须是合法字典值且满足状态机合法转移；终态不可再变。
     * 流转与日志同事务写入。
     */
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long id, TicketStatusChangeForm form) {
        ServiceTicket ticket = requireTicket(id);
        TicketStatus from = TicketStatus.of(ticket.getStatus());
        assertNotTerminal(ticket);
        if (!TicketStatus.isValid(form.getTargetStatus())) {
            throw new BizException("目标状态不合法: " + form.getTargetStatus());
        }
        TicketStatus to = TicketStatus.of(form.getTargetStatus());
        if (!from.canTransitionTo(to)) {
            throw new BizException("非法状态流转: %s → %s".formatted(from.label(), to.label()));
        }
        applyStatusChange(ticket, from, to, form.getRemark());
    }

    /**
     * 派单：指定受理人（用户必须存在且启用）。
     * PENDING → ASSIGNED 为一次合法状态流转；其余非终态为「改派」，状态不变但同样写日志。
     */
    @Transactional(rollbackFor = Exception.class)
    public void assign(Long id, TicketAssignForm form) {
        ServiceTicket ticket = requireTicket(id);
        TicketStatus from = TicketStatus.of(ticket.getStatus());
        assertNotTerminal(ticket);
        SysUser assignee = sysUserMapper.selectById(form.getOwnerId());
        if (assignee == null || assignee.getStatus() == null || assignee.getStatus() != 1) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "受理人不存在或已禁用");
        }
        ticket.setOwnerId(assignee.getId());
        ticket.setDeptId(assignee.getDeptId());
        String baseRemark = "指派受理人：" + assignee.getNickname();
        String remark = StringUtils.hasText(form.getRemark())
                ? baseRemark + "；" + form.getRemark() : baseRemark;
        if (from == TicketStatus.PENDING) {
            if (!from.canTransitionTo(TicketStatus.ASSIGNED)) {
                throw new BizException("非法状态流转: %s → %s".formatted(from.label(), TicketStatus.ASSIGNED.label()));
            }
            applyStatusChange(ticket, from, TicketStatus.ASSIGNED, remark);
        } else {
            // 改派：状态不变，仅更新受理人；记一条 from=to 的操作日志，保证操作可追溯
            if (!updateById(ticket)) {
                throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
            }
            writeLog(ticket.getId(), from, from, remark);
        }
    }

    /** 关闭工单：任意非终态 → CLOSED，写实际解决时间与流转日志。 */
    @Transactional(rollbackFor = Exception.class)
    public void close(Long id, TicketCloseForm form) {
        ServiceTicket ticket = requireTicket(id);
        TicketStatus from = TicketStatus.of(ticket.getStatus());
        assertNotTerminal(ticket);
        if (!from.canTransitionTo(TicketStatus.CLOSED)) {
            throw new BizException("当前状态「%s」不可关闭".formatted(from.label()));
        }
        applyStatusChange(ticket, from, TicketStatus.CLOSED,
                StringUtils.hasText(form.getRemark()) ? form.getRemark() : "关闭工单");
    }

    // ============================================================ SLA 超时扫描（系统上下文）

    /**
     * 扫描超时工单（由 ServiceSlaTask 在 DataScopeContext.runAsSystem 中调用）：
     * 对非终态且 SLA 超时的工单，置 overdue_flag=1，并写入 crm_reminder
     * （同业务旧提醒先清后写，避免重复堆积，参考 OpportunityServiceImpl.scanStaleOpportunities）。
     * 返回写入的提醒条数。
     */
    @Transactional(rollbackFor = Exception.class)
    public int scanOverdueTickets() {
        LocalDateTime now = LocalDateTime.now();
        List<ServiceTicket> active = list(Wrappers.<ServiceTicket>lambdaQuery()
                .in(ServiceTicket::getStatus, TicketStatus.nonTerminalCodes()));
        int written = 0;
        for (ServiceTicket ticket : active) {
            if (!computeOverdue(ticket, now)) {
                continue;
            }
            if (ticket.getOverdueFlag() == null || ticket.getOverdueFlag() != 1) {
                baseMapper.update(null, Wrappers.<ServiceTicket>lambdaUpdate()
                        .eq(ServiceTicket::getId, ticket.getId())
                        .set(ServiceTicket::getOverdueFlag, 1));
            }
            reminderMapper.delete(Wrappers.<Reminder>lambdaQuery()
                    .eq(Reminder::getBizType, SupportDicts.BIZ_TYPE_SLA_OVERDUE)
                    .eq(Reminder::getBizId, ticket.getId()));
            Reminder reminder = new Reminder();
            reminder.setBizType(SupportDicts.BIZ_TYPE_SLA_OVERDUE);
            reminder.setBizId(ticket.getId());
            reminder.setOwnerId(ticket.getOwnerId());
            reminder.setRemindLevel(SupportDicts.SLA_OVERDUE_REMIND_LEVEL);
            reminder.setRemindTime(now);
            reminder.setReadFlag(0);
            reminder.setContent("工单「%s」SLA 已超时，请尽快处理".formatted(ticket.getTicketNo()));
            reminderMapper.insert(reminder);
            written++;
        }
        return written;
    }

    // ============================================================ 私有工具

    /** 状态变更落地：更新状态 / 实际响应 / 实际解决时间 + 同事务写日志。 */
    private void applyStatusChange(ServiceTicket ticket, TicketStatus from, TicketStatus to, String remark) {
        LocalDateTime now = LocalDateTime.now();
        ticket.setStatus(to.code());
        // 首次离开「待受理」视为实际响应
        if (from == TicketStatus.PENDING && ticket.getActualResponseTime() == null) {
            ticket.setActualResponseTime(now);
        }
        // 进入终态（已完成 / 已关闭）记录实际解决时间
        if (to.isTerminal() && ticket.getActualResolveTime() == null) {
            ticket.setActualResolveTime(now);
        }
        if (!updateById(ticket)) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
        writeLog(ticket.getId(), from, to, remark);
    }

    /** 流转日志写入（与业务变更同事务）；无登录上下文时操作人留空。 */
    private void writeLog(Long ticketId, TicketStatus from, TicketStatus to, String remark) {
        ServiceTicketLog ticketLog = new ServiceTicketLog();
        ticketLog.setTicketId(ticketId);
        ticketLog.setFromStatus(from == null ? null : from.code());
        ticketLog.setToStatus(to.code());
        LoginUser operator = LoginUserContext.get();
        ticketLog.setOperatorId(operator == null ? null : operator.userId());
        ticketLog.setOperateTime(LocalDateTime.now());
        ticketLog.setRemark(remark);
        ticketLogMapper.insert(ticketLog);
    }

    private ServiceTicket requireTicket(Long id) {
        ServiceTicket ticket = getById(id);
        if (ticket == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "工单不存在");
        }
        return ticket;
    }

    private void assertNotTerminal(ServiceTicket ticket) {
        TicketStatus status = TicketStatus.of(ticket.getStatus());
        if (status.isTerminal()) {
            throw new BizException("工单已处于终态（%s），不可再变更".formatted(status.label()));
        }
    }

    private void assertContactInCustomer(Long contactId, Long customerId) {
        Contact contact = contactMapper.selectById(contactId);
        if (contact == null || !Objects.equals(contact.getCustomerId(), customerId)) {
            throw new BizException("联系人不存在或不属于当前客户");
        }
    }

    private SlaPolicy resolveSlaPolicy(String ticketType, String priority) {
        return slaPolicyMapper.selectOne(Wrappers.<SlaPolicy>lambdaQuery()
                .eq(SlaPolicy::getTicketType, ticketType)
                .eq(SlaPolicy::getPriority, priority)
                .eq(SlaPolicy::getEnabled, 1)
                .last("limit 1"));
    }

    /** 工单编号：ST + 日期 + UUID 片段；数据库 uk_ticket_no 唯一约束兜底。 */
    private String generateTicketNo() {
        return "ST" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    /**
     * 超时判定：应响应 / 应解决时间已到且未完成对应动作；已完成动作但晚于应完成时间也算超时。
     */
    private boolean computeOverdue(ServiceTicket ticket, LocalDateTime now) {
        boolean responseOverdue = ticket.getSlaDueResponseTime() != null
                && (ticket.getActualResponseTime() == null
                    ? ticket.getSlaDueResponseTime().isBefore(now)
                    : ticket.getActualResponseTime().isAfter(ticket.getSlaDueResponseTime()));
        boolean resolveOverdue = ticket.getSlaDueResolveTime() != null
                && (ticket.getActualResolveTime() == null
                    ? ticket.getSlaDueResolveTime().isBefore(now)
                    : ticket.getActualResolveTime().isAfter(ticket.getSlaDueResolveTime()));
        return responseOverdue || resolveOverdue;
    }

    private ServiceTicketVO toVO(ServiceTicket ticket) {
        ServiceTicketVO vo = new ServiceTicketVO();
        vo.setId(ticket.getId());
        vo.setTicketNo(ticket.getTicketNo());
        vo.setTitle(ticket.getTitle());
        vo.setCustomerId(ticket.getCustomerId());
        vo.setContactId(ticket.getContactId());
        vo.setOrderId(ticket.getOrderId());
        vo.setTicketType(ticket.getTicketType());
        vo.setTicketTypeName(SupportDicts.typeLabel(ticket.getTicketType()));
        vo.setPriority(ticket.getPriority());
        vo.setPriorityName(SupportDicts.priorityLabel(ticket.getPriority()));
        vo.setStatus(ticket.getStatus());
        vo.setStatusName(TicketStatus.labelOf(ticket.getStatus()));
        vo.setOwnerId(ticket.getOwnerId());
        vo.setDeptId(ticket.getDeptId());
        vo.setSlaDueResponseTime(ticket.getSlaDueResponseTime());
        vo.setSlaDueResolveTime(ticket.getSlaDueResolveTime());
        vo.setActualResponseTime(ticket.getActualResponseTime());
        vo.setActualResolveTime(ticket.getActualResolveTime());
        vo.setOverdueFlag(ticket.getOverdueFlag());
        vo.setOverdue(computeOverdue(ticket, LocalDateTime.now()));
        vo.setSource(ticket.getSource());
        vo.setSourceName(SupportDicts.sourceLabel(ticket.getSource()));
        vo.setDescription(ticket.getDescription());
        vo.setCreateTime(ticket.getCreateTime());
        vo.setUpdateTime(ticket.getUpdateTime());
        vo.setVersion(ticket.getVersion());
        return vo;
    }

    /** 批量补客户 / 联系人 / 受理人名称，避免逐行查库 N+1。 */
    private void fillNames(List<ServiceTicketVO> vos, List<ServiceTicket> tickets) {
        List<Long> customerIds = tickets.stream().map(ServiceTicket::getCustomerId)
                .filter(Objects::nonNull).distinct().toList();
        List<Long> contactIds = tickets.stream().map(ServiceTicket::getContactId)
                .filter(Objects::nonNull).distinct().toList();
        List<Long> ownerIds = tickets.stream().map(ServiceTicket::getOwnerId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, String> customerMap = customerIds.isEmpty() ? Map.of()
                : customerMapper.selectBatchIds(customerIds).stream()
                .collect(Collectors.toMap(Customer::getId, Customer::getName));
        Map<Long, String> contactMap = contactIds.isEmpty() ? Map.of()
                : contactMapper.selectBatchIds(contactIds).stream()
                .collect(Collectors.toMap(Contact::getId, Contact::getName));
        Map<Long, String> ownerMap = ownerIds.isEmpty() ? Map.of()
                : sysUserMapper.selectBatchIds(ownerIds).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getNickname));
        for (ServiceTicketVO vo : vos) {
            vo.setCustomerName(customerMap.get(vo.getCustomerId()));
            vo.setContactName(vo.getContactId() == null ? null : contactMap.get(vo.getContactId()));
            vo.setOwnerName(vo.getOwnerId() == null ? null : ownerMap.get(vo.getOwnerId()));
        }
    }
}
