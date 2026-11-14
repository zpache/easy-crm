package cn.easykits.crm.modules.opportunity.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.ResultCode;
import cn.easykits.crm.common.context.LoginUser;
import cn.easykits.crm.common.context.LoginUserContext;
import cn.easykits.crm.common.exception.BizException;
import cn.easykits.crm.entity.Customer;
import cn.easykits.crm.entity.Opportunity;
import cn.easykits.crm.entity.OpportunityStage;
import cn.easykits.crm.mapper.ContactMapper;
import cn.easykits.crm.mapper.CustomerMapper;
import cn.easykits.crm.modules.customer.constant.CustomerDicts;
import cn.easykits.crm.modules.opportunity.OpportunityService;
import cn.easykits.crm.modules.opportunity.constant.LostReasons;
import cn.easykits.crm.modules.opportunity.dto.ForecastQuery;
import cn.easykits.crm.modules.opportunity.dto.OpportunityCreateForm;
import cn.easykits.crm.modules.opportunity.dto.OpportunityQuery;
import cn.easykits.crm.modules.opportunity.dto.OpportunityUpdateForm;
import cn.easykits.crm.modules.opportunity.dto.StageChangeForm;
import cn.easykits.crm.modules.opportunity.dto.StageForm;
import cn.easykits.crm.modules.opportunity.event.OpportunityStageChangedEvent;
import cn.easykits.crm.modules.opportunity.event.OpportunityWonEvent;
import cn.easykits.crm.modules.opportunity.vo.BoardStage;
import cn.easykits.crm.modules.opportunity.vo.ForecastVO;
import cn.easykits.crm.modules.opportunity.vo.OpportunityVO;
import cn.easykits.crm.modules.opportunity.vo.StageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 商机服务：领域接口（线索转化创建）+ 管理端 CRUD / 阶段流转 / 看板 / 预测 / 阶段配置。
 * <p>终态（赢单/丢单）校验在 Service 层强制执行；阶段变更/赢单事件统一在
 * 事务提交后（AFTER_COMMIT）由 {@code OpportunityEventListener} 消费。
 * <p>行级数据权限：crm_opportunity 已注册进 DataScopeTableRegistry（owner_id + dept_id）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpportunityServiceImpl extends ServiceImpl<cn.easykits.crm.mapper.OpportunityMapper, Opportunity>
        implements OpportunityService {

    /** 停滞阈值（天）：7 黄 / 14 橙 / 30 红 */
    private static final int STALE_YELLOW_DAYS = 7;
    private static final int STALE_ORANGE_DAYS = 14;
    private static final int STALE_RED_DAYS = 30;

    private final ContactMapper contactMapper;
    private final cn.easykits.crm.mapper.ReminderMapper reminderMapper;
    private final CustomerMapper customerMapper;
    private final cn.easykits.crm.mapper.OpportunityStageMapper stageMapper;
    private final cn.easykits.crm.mapper.SysUserMapper sysUserMapper;
    private final cn.easykits.crm.mapper.SysDeptMapper sysDeptMapper;
    private final cn.easykits.crm.mapper.OpportunityStageLogMapper stageLogMapper;
    private final ApplicationEventPublisher eventPublisher;

    // ============================================================ 领域接口（线索转化）

    /**
     * 由线索转化创建商机：阶段取启用阶段中 sort_no 最小的（通常为「初步接洽」），
     * win_rate 冗余阶段默认赢单率，last_activity_at 置为当前时间。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(OpportunityInfo info) {
        OpportunityStage stage = stageMapper.selectList(Wrappers.<OpportunityStage>lambdaQuery()
                        .eq(OpportunityStage::getEnabled, 1)
                        .eq(OpportunityStage::getIsWon, 0)
                        .eq(OpportunityStage::getIsLost, 0)
                        .orderByAsc(OpportunityStage::getSortNo))
                .stream().findFirst()
                .orElseThrow(() -> new BizException("没有可用的销售阶段配置，请先在阶段管理中初始化"));
        Opportunity opportunity = new Opportunity();
        opportunity.setName(info.name());
        opportunity.setCustomerId(info.customerId());
        opportunity.setContactId(info.contactId());
        opportunity.setLeadId(info.leadId());
        opportunity.setStageCode(stage.getStageCode());
        opportunity.setWinRate(stage.getDefaultWinRate());
        opportunity.setCurrency("CNY");
        opportunity.setOwnerId(info.ownerId());
        opportunity.setDeptId(info.deptId());
        opportunity.setLastActivityAt(LocalDateTime.now());
        save(opportunity);
        return opportunity.getId();
    }

    // ============================================================ 管理端 CRUD

    /**
     * 创建商机（管理端）：客户必须存在；阶段同转化链路取首个启用阶段。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createFromForm(OpportunityCreateForm form) {
        Customer customer = customerMapper.selectById(form.getCustomerId());
        if (customer == null || customer.getStatus() == 3) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "所属客户不存在或已合并");
        }
        if (form.getContactId() != null) {
            assertContactInCustomer(form.getContactId(), form.getCustomerId());
        }
        LoginUser user = LoginUserContext.require();
        OpportunityInfo info = new OpportunityInfo(form.getName(), null,
                form.getCustomerId(), form.getContactId(), user.userId(), user.deptId());
        Long id = create(info);
        Opportunity opportunity = getById(id);
        opportunity.setAmount(form.getAmount());
        opportunity.setExpectedCloseDate(form.getExpectedCloseDate());
        updateById(opportunity);
        return id;
    }

    /**
     * 更新商机基本信息（阶段变更走专用接口）；赢单/丢单终态后基本信息不可改。
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateFromForm(Long id, OpportunityUpdateForm form) {
        Opportunity opportunity = getById(id);
        if (opportunity == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "商机不存在");
        }
        assertNotTerminal(opportunity);
        if (StringUtils.hasText(form.getName())) {
            opportunity.setName(form.getName());
        }
        if (form.getContactId() != null) {
            assertContactInCustomer(form.getContactId(), opportunity.getCustomerId());
            opportunity.setContactId(form.getContactId());
        }
        opportunity.setAmount(form.getAmount() != null ? form.getAmount() : opportunity.getAmount());
        opportunity.setExpectedCloseDate(form.getExpectedCloseDate() != null
                ? form.getExpectedCloseDate() : opportunity.getExpectedCloseDate());
        opportunity.setLastActivityAt(LocalDateTime.now());
        if (!updateById(opportunity)) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    /**
     * 分页查询（customerId/stageCode/ownerId/预计成交时间范围过滤）。
     */
    public PageResult<OpportunityVO> pageQuery(OpportunityQuery query) {
        Page<Opportunity> page = page(Page.of(query.getPageNum(), query.getPageSize()),
                Wrappers.<Opportunity>lambdaQuery()
                        .eq(query.getCustomerId() != null, Opportunity::getCustomerId, query.getCustomerId())
                        .eq(StringUtils.hasText(query.getStageCode()), Opportunity::getStageCode, query.getStageCode())
                        .isNull(query.getOwnerId() != null && query.getOwnerId() == -1, Opportunity::getOwnerId)
                        .eq(query.getOwnerId() != null && query.getOwnerId() != -1,
                                Opportunity::getOwnerId, query.getOwnerId())
                        .ge(query.getStartDate() != null, Opportunity::getExpectedCloseDate, query.getStartDate())
                        .le(query.getEndDate() != null, Opportunity::getExpectedCloseDate, query.getEndDate())
                        .orderByDesc(Opportunity::getId));
        List<OpportunityVO> vos = page.getRecords().stream().map(this::toVO).toList();
        fillCustomerContactOwnerNames(vos, page.getRecords());
        return new PageResult<>(vos, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    /**
     * 详情。
     */
    public OpportunityVO detail(Long id) {
        Opportunity opportunity = getById(id);
        if (opportunity == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "商机不存在");
        }
        OpportunityVO vo = toVO(opportunity);
        fillCustomerContactOwnerNames(List.of(vo), List.of(opportunity));
        return vo;
    }

    /**
     * 看板：一次查询全部商机 + 一次查询阶段配置，按阶段分组返回（禁止前端循环调接口）。
     */
    public List<BoardStage> board() {
        List<OpportunityStage> stages = stageMapper.selectList(Wrappers.<OpportunityStage>lambdaQuery()
                .orderByAsc(OpportunityStage::getSortNo));
        List<Opportunity> all = list(Wrappers.<Opportunity>lambdaQuery()
                .orderByDesc(Opportunity::getId));
        Map<String, List<Opportunity>> byStage = all.stream()
                .collect(Collectors.groupingBy(Opportunity::getStageCode));
        List<BoardStage> result = new ArrayList<>();
        for (OpportunityStage stage : stages) {
            List<OpportunityVO> cards = byStage
                    .getOrDefault(stage.getStageCode(), List.of())
                    .stream().map(this::toVO).toList();
            fillCustomerContactOwnerNames(cards, byStage.getOrDefault(stage.getStageCode(), List.of()));
            BoardStage bs = new BoardStage();
            bs.setStageCode(stage.getStageCode());
            bs.setStageName(stage.getStageName());
            bs.setSortNo(stage.getSortNo());
            bs.setDefaultWinRate(stage.getDefaultWinRate());
            bs.setWon(stage.getIsWon() != null && stage.getIsWon() == 1);
            bs.setLost(stage.getIsLost() != null && stage.getIsLost() == 1);
            bs.setEnabled(stage.getEnabled() != null && stage.getEnabled() == 1);
            bs.setOpportunities(cards);
            result.add(bs);
        }
        return result;
    }

    /**
     * 阶段变更：终态不可变更、目标阶段必须存在且启用；赢单回写实际成交信息并发布
     * OpportunityWonEvent，丢单必填 lostReason；任意变更发布 OpportunityStageChangedEvent。
     * 事件统一 AFTER_COMMIT 触发。
     */
    @Transactional(rollbackFor = Exception.class)
    public void changeStage(Long id, StageChangeForm form) {
        Opportunity opportunity = getById(id);
        if (opportunity == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "商机不存在");
        }
        OpportunityStage from = requireStage(opportunity.getStageCode());
        OpportunityStage to = requireStage(form.getTargetStageCode());
        // 终态校验（Service 层强制，不只靠前端）
        if (isTerminal(from)) {
            throw new BizException("当前阶段「%s」为终态（%s），不可再变更"
                    .formatted(from.getStageName(), from.getIsWon() == 1 ? "赢单" : "丢单"));
        }
        if (to.getEnabled() == null || to.getEnabled() != 1) {
            throw new BizException("目标阶段「%s」已停用".formatted(to.getStageName()));
        }

        opportunity.setStageCode(to.getStageCode());
        opportunity.setWinRate(to.getDefaultWinRate());
        opportunity.setLastActivityAt(LocalDateTime.now());

        if (to.getIsWon() != null && to.getIsWon() == 1) {
            // 赢单：回写实际成交信息（缺省今天 / 商机预计金额）
            opportunity.setActualCloseDate(form.getActualCloseDate() != null
                    ? form.getActualCloseDate() : LocalDate.now());
            opportunity.setActualAmount(form.getActualAmount() != null
                    ? form.getActualAmount() : opportunity.getAmount());
            // 客户标记为成交客户（状态 4）
            Customer customer = customerMapper.selectById(opportunity.getCustomerId());
            if (customer != null) {
                customer.setStatus(4);
                customerMapper.updateById(customer);
            }
        }
        if (to.getIsLost() != null && to.getIsLost() == 1) {
            // 丢单：lostReason 必填（字典校验），competitor 可选
            if (!StringUtils.hasText(form.getLostReason())) {
                throw new BizException("丢单必须填写丢单原因");
            }
            if (!LostReasons.isValid(form.getLostReason())) {
                throw new BizException("丢单原因字典值不合法: " + form.getLostReason());
            }
            opportunity.setLostReason(form.getLostReason());
            opportunity.setCompetitor(StringUtils.hasText(form.getCompetitor())
                    ? form.getCompetitor() : null);
        }

        if (!updateById(opportunity)) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }

        // 阶段变更日志：与阶段变更同一事务写入，时间线视图据此还原阶段历史
        cn.easykits.crm.entity.OpportunityStageLog stageLog = new cn.easykits.crm.entity.OpportunityStageLog();
        stageLog.setOpportunityId(opportunity.getId());
        stageLog.setFromStage(from.getStageCode());
        stageLog.setToStage(to.getStageCode());
        LoginUser operator = LoginUserContext.get();
        stageLog.setOperatorId(operator == null ? opportunity.getOwnerId() : operator.userId());
        stageLog.setChangeTime(LocalDateTime.now());
        stageLogMapper.insert(stageLog);

        // 事件：事务提交后由 AFTER_COMMIT 监听器消费
        eventPublisher.publishEvent(new OpportunityStageChangedEvent(
                opportunity.getId(), opportunity.getName(),
                from.getStageCode(), to.getStageCode(), opportunity.getOwnerId()));
        if (to.getIsWon() != null && to.getIsWon() == 1) {
            eventPublisher.publishEvent(new OpportunityWonEvent(
                    opportunity.getId(), opportunity.getName(), opportunity.getCustomerId(),
                    opportunity.getOwnerId(), opportunity.getActualAmount(),
                    opportunity.getActualCloseDate()));
        }
    }

    // ============================================================ 销售预测

    /**
     * 销售预测：一条聚合 SQL 按阶段汇总（排除赢单/丢单终态），
     * totalForecast 为聚合结果各行求和（行数 = 阶段数，非按商机循环）。
     */
    public ForecastVO forecast(ForecastQuery query) {
        List<ForecastVO.StageRow> rows = baseMapper.forecastByStage(
                query.getDeptId(), query.getStartDate(), query.getEndDate());
        BigDecimal total = rows.stream()
                .map(ForecastVO.StageRow::getForecastAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long count = rows.stream().mapToLong(ForecastVO.StageRow::getCnt).sum();
        ForecastVO vo = new ForecastVO();
        vo.setRows(rows);
        vo.setTotalForecast(total);
        vo.setTotalCount(count);
        return vo;
    }

    // ============================================================ 阶段配置

    /**
     * 阶段配置列表（全部，含停用）。
     */
    public List<StageVO> listStages() {
        return stageMapper.selectList(Wrappers.<OpportunityStage>lambdaQuery()
                        .orderByAsc(OpportunityStage::getSortNo)
                        .orderByAsc(OpportunityStage::getId))
                .stream().map(this::toStageVO).toList();
    }

    /**
     * 新增阶段配置：stage_code 唯一。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createStage(StageForm form) {
        long exists = stageMapper.selectCount(Wrappers.<OpportunityStage>lambdaQuery()
                .eq(OpportunityStage::getStageCode, form.getStageCode()));
        if (exists > 0) {
            throw new BizException("阶段编码已存在: " + form.getStageCode());
        }
        OpportunityStage stage = new OpportunityStage();
        applyStageForm(stage, form);
        stageMapper.insert(stage);
        return stage.getId();
    }

    /**
     * 修改阶段配置。停用阶段时若有商机停留在该阶段则拒绝，避免商机悬空。
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateStage(Long id, StageForm form) {
        OpportunityStage stage = stageMapper.selectById(id);
        if (stage == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "阶段配置不存在");
        }
        OpportunityStage sameCode = stageMapper.selectOne(Wrappers.<OpportunityStage>lambdaQuery()
                .eq(OpportunityStage::getStageCode, form.getStageCode())
                .ne(OpportunityStage::getId, id)
                .last("limit 1"), false);
        if (sameCode != null) {
            throw new BizException("阶段编码已存在: " + form.getStageCode());
        }
        boolean disabling = stage.getEnabled() == 1 && form.getEnabled() != null && form.getEnabled() == 0;
        if (disabling) {
            Long occupied = baseMapper.selectCount(Wrappers.<Opportunity>lambdaQuery()
                    .eq(Opportunity::getStageCode, stage.getStageCode()));
            if (occupied != null && occupied > 0) {
                throw new BizException("仍有 %d 个商机停留在该阶段，请先变更阶段后再停用".formatted(occupied));
            }
        }
        applyStageForm(stage, form);
        stageMapper.updateById(stage);
    }

    // ============================================================ 停滞检测

    /**
     * 停滞等级：距 last_activity_at 超过 30 天红(3)、14 天橙(2)、7 天黄(1)，否则 0。
     */
    public static int staleLevelOf(LocalDateTime lastActivityAt) {
        if (lastActivityAt == null) {
            return 3;
        }
        long days = ChronoUnit.DAYS.between(lastActivityAt, LocalDateTime.now());
        if (days >= STALE_RED_DAYS) {
            return 3;
        }
        if (days >= STALE_ORANGE_DAYS) {
            return 2;
        }
        if (days >= STALE_YELLOW_DAYS) {
            return 1;
        }
        return 0;
    }

    /**
     * 停滞扫描（定时任务调用）：对未终态且停滞的商机写 crm_reminder，
     * 先清同业务旧提醒再写当前等级，避免重复堆积。
     */
    @Transactional(rollbackFor = Exception.class)
    public int scanStaleOpportunities() {
        List<Opportunity> active = list(Wrappers.<Opportunity>lambdaQuery());
        int written = 0;
        for (Opportunity opportunity : active) {
            OpportunityStage stage = stageMapper.selectOne(Wrappers.<OpportunityStage>lambdaQuery()
                    .eq(OpportunityStage::getStageCode, opportunity.getStageCode())
                    .last("limit 1"));
            if (stage != null && (isTerminal(stage)
                    || (stage.getEnabled() != null && stage.getEnabled() != 1))) {
                continue;
            }
            int level = staleLevelOf(opportunity.getLastActivityAt());
            if (level == 0) {
                continue;
            }
            reminderMapper.delete(Wrappers.<cn.easykits.crm.entity.Reminder>lambdaQuery()
                    .eq(cn.easykits.crm.entity.Reminder::getBizType, BIZ_TYPE_STALE)
                    .eq(cn.easykits.crm.entity.Reminder::getBizId, opportunity.getId()));
            cn.easykits.crm.entity.Reminder reminder = new cn.easykits.crm.entity.Reminder();
            reminder.setBizType(BIZ_TYPE_STALE);
            reminder.setBizId(opportunity.getId());
            reminder.setOwnerId(opportunity.getOwnerId());
            reminder.setRemindLevel(level);
            reminder.setRemindTime(LocalDateTime.now());
            reminder.setReadFlag(0);
            reminder.setContent("商机「%s」已停滞（等级 %d）".formatted(opportunity.getName(), level));
            reminderMapper.insert(reminder);
            written++;
        }
        return written;
    }

    /** 停滞提醒业务类型 */
    public static final String BIZ_TYPE_STALE = "opportunity_stale";

    // ============================================================ 私有工具

    private void assertContactInCustomer(Long contactId, Long customerId) {
        cn.easykits.crm.entity.Contact contact = contactMapper.selectById(contactId);
        if (contact == null || !Objects.equals(contact.getCustomerId(), customerId)) {
            throw new BizException("联系人不存在或不属于当前客户");
        }
    }

    private void assertNotTerminal(Opportunity opportunity) {
        OpportunityStage stage = requireStage(opportunity.getStageCode());
        if (isTerminal(stage)) {
            throw new BizException("商机已处于终态（%s），基本信息不可修改"
                    .formatted(stage.getStageName()));
        }
    }

    private boolean isTerminal(OpportunityStage stage) {
        return (stage.getIsWon() != null && stage.getIsWon() == 1)
                || (stage.getIsLost() != null && stage.getIsLost() == 1);
    }

    private OpportunityStage requireStage(String stageCode) {
        if (!StringUtils.hasText(stageCode)) {
            throw new BizException("阶段编码不能为空");
        }
        OpportunityStage stage = stageMapper.selectOne(Wrappers.<OpportunityStage>lambdaQuery()
                .eq(OpportunityStage::getStageCode, stageCode)
                .last("limit 1"));
        if (stage == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "阶段不存在: " + stageCode);
        }
        return stage;
    }

    private void applyStageForm(OpportunityStage stage, StageForm form) {
        stage.setStageCode(form.getStageCode());
        stage.setStageName(form.getStageName());
        stage.setSortNo(form.getSortNo());
        stage.setDefaultWinRate(form.getDefaultWinRate());
        stage.setIsWon(Boolean.TRUE.equals(form.getIsWon()) || Integer.valueOf(1).equals(form.getIsWon()) ? 1 : 0);
        stage.setIsLost(Boolean.TRUE.equals(form.getIsLost()) || Integer.valueOf(1).equals(form.getIsLost()) ? 1 : 0);
        stage.setEnabled(form.getEnabled() == null ? 1 : form.getEnabled());
    }

    private StageVO toStageVO(OpportunityStage stage) {
        StageVO vo = new StageVO();
        vo.setId(stage.getId());
        vo.setStageCode(stage.getStageCode());
        vo.setStageName(stage.getStageName());
        vo.setSortNo(stage.getSortNo());
        vo.setDefaultWinRate(stage.getDefaultWinRate());
        vo.setIsWon(stage.getIsWon() != null && stage.getIsWon() == 1);
        vo.setIsLost(stage.getIsLost() != null && stage.getIsLost() == 1);
        vo.setEnabled(stage.getEnabled() != null && stage.getEnabled() == 1);
        return vo;
    }

    private OpportunityVO toVO(Opportunity opportunity) {
        OpportunityVO vo = new OpportunityVO();
        vo.setId(opportunity.getId());
        vo.setName(opportunity.getName());
        vo.setCustomerId(opportunity.getCustomerId());
        vo.setContactId(opportunity.getContactId());
        vo.setLeadId(opportunity.getLeadId());
        vo.setStageCode(opportunity.getStageCode());
        OpportunityStage stage = stageMapper.selectOne(Wrappers.<OpportunityStage>lambdaQuery()
                .eq(OpportunityStage::getStageCode, opportunity.getStageCode())
                .last("limit 1"));
        vo.setStageName(stage == null ? opportunity.getStageCode() : stage.getStageName());
        vo.setAmount(opportunity.getAmount());
        vo.setCurrency(opportunity.getCurrency());
        vo.setWinRate(opportunity.getWinRate());
        vo.setExpectedCloseDate(opportunity.getExpectedCloseDate());
        vo.setActualCloseDate(opportunity.getActualCloseDate());
        vo.setActualAmount(opportunity.getActualAmount());
        vo.setLostReason(opportunity.getLostReason());
        vo.setLostReasonName(LostReasons.labelOf(opportunity.getLostReason()));
        vo.setCompetitor(opportunity.getCompetitor());
        vo.setOwnerId(opportunity.getOwnerId());
        vo.setDeptId(opportunity.getDeptId());
        vo.setStaleLevel(staleLevelOf(opportunity.getLastActivityAt()));
        vo.setLastActivityAt(opportunity.getLastActivityAt());
        vo.setCreateTime(opportunity.getCreateTime());
        vo.setUpdateTime(opportunity.getUpdateTime());
        vo.setVersion(opportunity.getVersion());
        return vo;
    }

    private void fillCustomerContactOwnerNames(List<OpportunityVO> vos, List<Opportunity> opportunities) {
        List<Long> customerIds = opportunities.stream().map(Opportunity::getCustomerId)
                .filter(Objects::nonNull).distinct().toList();
        List<Long> contactIds = opportunities.stream().map(Opportunity::getContactId)
                .filter(Objects::nonNull).distinct().toList();
        List<Long> ownerIds = opportunities.stream().map(Opportunity::getOwnerId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, String> customerMap = customerIds.isEmpty() ? Map.of()
                : customerMapper.selectBatchIds(customerIds).stream()
                .collect(Collectors.toMap(Customer::getId, Customer::getName));
        Map<Long, String> contactMap = contactIds.isEmpty() ? Map.of()
                : contactMapper.selectBatchIds(contactIds).stream()
                .collect(Collectors.toMap(cn.easykits.crm.entity.Contact::getId,
                        cn.easykits.crm.entity.Contact::getName));
        Map<Long, String> ownerMap = ownerIds.isEmpty() ? Map.of()
                : sysUserMapper.selectBatchIds(ownerIds).stream()
                .collect(Collectors.toMap(u -> u.getId(), u -> u.getNickname()));
        for (OpportunityVO vo : vos) {
            vo.setCustomerName(customerMap.get(vo.getCustomerId()));
            vo.setContactName(vo.getContactId() == null ? null : contactMap.get(vo.getContactId()));
            vo.setOwnerName(vo.getOwnerId() == null ? null : ownerMap.get(vo.getOwnerId()));
        }
    }

}
