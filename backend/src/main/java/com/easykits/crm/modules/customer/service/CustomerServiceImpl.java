package com.easykits.crm.modules.customer.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.easykits.crm.common.api.PageResult;
import com.easykits.crm.common.api.ResultCode;
import com.easykits.crm.common.exception.BizException;
import com.easykits.crm.entity.Contact;
import com.easykits.crm.entity.Customer;
import com.easykits.crm.entity.Lead;
import com.easykits.crm.mapper.ContactMapper;
import com.easykits.crm.mapper.CustomerMapper;
import com.easykits.crm.mapper.LeadMapper;
import com.easykits.crm.modules.customer.CustomerService;
import com.easykits.crm.modules.customer.constant.CustomerDicts;
import com.easykits.crm.modules.customer.dto.CustomerCreateForm;
import com.easykits.crm.modules.customer.dto.CustomerInfo;
import com.easykits.crm.modules.customer.dto.CustomerQuery;
import com.easykits.crm.modules.customer.dto.MergeForm;
import com.easykits.crm.modules.customer.vo.Customer360VO;
import com.easykits.crm.modules.customer.vo.CustomerVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 客户服务：领域接口（线索转化）+ 管理端 CRUD / 合并 / 360 / 公海 的统一实现。
 * <p>行级数据权限：crm_customer 已注册进 DataScopeTableRegistry（owner_id + dept_id）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl extends ServiceImpl<CustomerMapper, Customer>
        implements CustomerService {

    public static final int STATUS_NORMAL = 1;
    public static final int STATUS_POOL = 2;
    public static final int STATUS_MERGED = 3;

    private final ContactMapper contactMapper;
    private final LeadMapper leadMapper;
    private final com.easykits.crm.mapper.SysUserMapper sysUserMapper;
    private final com.easykits.crm.mapper.SysDeptMapper sysDeptMapper;

    // ============================================================ 领域接口（线索转化）

    /**
     * 创建客户（转化链路）：名称查重（信用代码缺省按名称匹配），重复抛异常并携带已存在 ID。
     * phone / email / address 本轮无对应列（TODO：客户明细字段扩展后落库）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CustomerInfo info, Long ownerId, Long deptId) {
        Customer existed = findDuplicate(info.getName(), null, null);
        if (existed != null) {
            throw new BizException("已存在同名称客户（ID: %d），可通过「关联已有客户」完成转化"
                    .formatted(existed.getId()));
        }
        Customer customer = new Customer();
        customer.setName(info.getName());
        customer.setStatus(STATUS_NORMAL);
        customer.setOwnerId(ownerId);
        customer.setDeptId(deptId);
        customer.setLastFollowTime(LocalDateTime.now());
        save(customer);
        return customer.getId();
    }

    @Override
    public Long linkExisting(Long customerId) {
        if (customerId == null) {
            throw new BizException("客户 ID 不能为空");
        }
        Customer customer = getById(customerId);
        if (customer == null || customer.getStatus() == STATUS_MERGED) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "客户不存在或已合并");
        }
        return customerId;
    }

    // ============================================================ 管理端 CRUD

    /**
     * 创建客户（管理端）：名称 + 信用代码组合查重，重复阻断并提示已存在客户 ID。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createFromForm(CustomerCreateForm form) {
        Customer existed = findDuplicate(form.getName(), form.getCreditCode(), null);
        if (existed != null) {
            throw new BizException("已存在相同名称%s的客户（ID: %d）"
                    .formatted(StringUtils.hasText(form.getCreditCode()) ? "及信用代码" : "",
                            existed.getId()));
        }
        Customer customer = new Customer();
        applyForm(customer, form.getName(), form.getCreditCode(), form.getIndustry(),
                form.getScale(), form.getRegion(), form.getLevel(), form.getRemark());
        customer.setStatus(STATUS_NORMAL);
        customer.setOwnerId(form.getOwnerId());
        customer.setDeptId(form.getDeptId());
        customer.setLastFollowTime(LocalDateTime.now());
        save(customer);
        return customer.getId();
    }

    /**
     * 更新客户（已合并不可编辑）；名称 / 信用代码变更时重新查重。
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateFromForm(Long id, CustomerCreateForm form) {
        Customer customer = getById(id);
        if (customer == null || customer.getStatus() == STATUS_MERGED) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "客户不存在或已合并");
        }
        Customer existed = findDuplicate(form.getName(), form.getCreditCode(), id);
        if (existed != null) {
            throw new BizException("已存在相同名称%s的客户（ID: %d）"
                    .formatted(StringUtils.hasText(form.getCreditCode()) ? "及信用代码" : "",
                            existed.getId()));
        }
        applyForm(customer, form.getName(), form.getCreditCode(), form.getIndustry(),
                form.getScale(), form.getRegion(), form.getLevel(), form.getRemark());
        if (!updateById(customer)) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    /**
     * 分页查询（name/industry/status/ownerId 过滤）。已合并（status=3）不在默认结果中，
     * 仅当显式传 status=3 时可查；行级数据权限由拦截器注入。
     */
    public PageResult<CustomerVO> pageQuery(CustomerQuery query) {
        Page<Customer> page = page(Page.of(query.getPageNum(), query.getPageSize()),
                Wrappers.<Customer>lambdaQuery()
                        .like(StringUtils.hasText(query.getName()), Customer::getName, query.getName())
                        .eq(query.getIndustry() != null, Customer::getIndustry, query.getIndustry())
                        // 默认排除已合并；显式传 status 才可查已合并
                        .ne(query.getStatus() == null, Customer::getStatus, STATUS_MERGED)
                        .eq(query.getStatus() != null, Customer::getStatus, query.getStatus())
                        // ownerId = -1 查公海
                        .isNull(query.getOwnerId() != null && query.getOwnerId() == -1, Customer::getOwnerId)
                        .eq(query.getOwnerId() != null && query.getOwnerId() != -1,
                                Customer::getOwnerId, query.getOwnerId())
                        .orderByDesc(Customer::getId));
        List<CustomerVO> vos = page.getRecords().stream().map(this::toVO).toList();
        fillOwnerDeptNames(vos, page.getRecords());
        return new PageResult<>(vos, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    /**
     * 详情。
     */
    public CustomerVO detail(Long id) {
        Customer customer = getById(id);
        if (customer == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "客户不存在");
        }
        CustomerVO vo = toVO(customer);
        fillOwnerDeptNames(List.of(vo), List.of(customer));
        return vo;
    }

    /**
     * 客户 360 视图：固定 2 次查询（客户 + 联系人），商机 / 跟进记录轮次 3/4 后填充，
     * 当前返回空数组——查询次数不随联系人数增长。
     */
    public Customer360VO view360(Long id) {
        Customer customer = getById(id);
        if (customer == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "客户不存在");
        }
        CustomerVO customerVO = toVO(customer);
        fillOwnerDeptNames(List.of(customerVO), List.of(customer));
        // 主联系人排最前
        List<Contact> contacts = contactMapper.selectList(Wrappers.<Contact>lambdaQuery()
                .eq(Contact::getCustomerId, id)
                .orderByDesc(Contact::getIsPrimary)
                .orderByAsc(Contact::getId));
        List<Map<String, Object>> contactList = contacts.stream()
                .map(c -> Map.<String, Object>of(
                        "id", c.getId(),
                        "name", c.getName(),
                        "phone", c.getPhone() == null ? "" : c.getPhone(),
                        "email", c.getEmail() == null ? "" : c.getEmail(),
                        "position", c.getPosition() == null ? "" : c.getPosition(),
                        "isPrimary", c.getIsPrimary() != null && c.getIsPrimary() == 1))
                .toList();
        Customer360VO vo = new Customer360VO();
        vo.setCustomer(customerVO);
        vo.setContacts(contactList);
        // TODO（轮次 3 商机表建立后）：opportunities 按 customer_id 查询填充
        vo.setOpportunities(List.of());
        // TODO（轮次 4 跟进记录表建立后）：followRecords 按 related_customer_id 查询填充
        vo.setFollowRecords(List.of());
        return vo;
    }

    /**
     * 客户合并：B（source）合并到 A（target）。事务内迁移联系人、线索转化关联；
     * 商机 / 跟进记录迁移 TODO（轮次 3 / 4）。合并后 B 标记已合并，不再出现在默认列表。
     */
    @Transactional(rollbackFor = Exception.class)
    public void merge(Long sourceId, Long targetId) {
        if (sourceId.equals(targetId)) {
            throw new BizException("合并目标不能是客户自身");
        }
        Customer source = getById(sourceId);
        Customer target = getById(targetId);
        if (source == null || source.getStatus() == STATUS_MERGED) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "被合并客户不存在或已合并");
        }
        if (target == null || target.getStatus() == STATUS_MERGED) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "合并目标客户不存在或已合并");
        }

        // 1. 迁移联系人
        contactMapper.update(null, Wrappers.<Contact>lambdaUpdate()
                .eq(Contact::getCustomerId, sourceId)
                .set(Contact::getCustomerId, targetId));
        // 目标客户已存在主联系人时，迁移过来的主联系人降级为普通联系人，保证主联系人唯一
        Long targetPrimary = contactMapper.selectCount(Wrappers.<Contact>lambdaQuery()
                .eq(Contact::getCustomerId, targetId)
                .eq(Contact::getIsPrimary, 1));
        if (targetPrimary != null && targetPrimary > 1) {
            Contact keep = contactMapper.selectOne(Wrappers.<Contact>lambdaQuery()
                    .eq(Contact::getCustomerId, targetId)
                    .eq(Contact::getIsPrimary, 1)
                    .orderByDesc(Contact::getUpdateTime)
                    .last("limit 1"));
            contactMapper.update(null, Wrappers.<Contact>lambdaUpdate()
                    .eq(Contact::getCustomerId, targetId)
                    .eq(Contact::getIsPrimary, 1)
                    .ne(keep != null, Contact::getId, keep == null ? -1 : keep.getId())
                    .set(Contact::getIsPrimary, 0));
        }

        // 2. 迁移线索的转化关联字段
        leadMapper.update(null, Wrappers.<Lead>lambdaUpdate()
                .eq(Lead::getConvertedCustomerId, sourceId)
                .set(Lead::getConvertedCustomerId, targetId));

        // TODO（轮次 3 商机表建立后）：UPDATE crm_opportunity SET customer_id = target WHERE customer_id = source
        // TODO（轮次 4 跟进记录表建立后）：UPDATE crm_follow_record SET customer_id = target WHERE customer_id = source

        // 3. 源客户标记已合并
        source.setStatus(STATUS_MERGED);
        source.setMergedToId(targetId);
        if (!updateById(source)) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
        log.info("客户合并完成: source={} -> target={}", sourceId, targetId);
    }

    // ============================================================ 公海

    /**
     * 放入公海：仅正常状态客户；owner / dept 置空，状态置公海。
     */
    @Transactional(rollbackFor = Exception.class)
    public void toPool(Long id) {
        Customer customer = getById(id);
        if (customer == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "客户不存在");
        }
        if (customer.getStatus() == STATUS_MERGED) {
            throw new BizException("已合并客户不可操作");
        }
        if (customer.getStatus() == STATUS_POOL) {
            throw new BizException("该客户已在公海");
        }
        customer.setStatus(STATUS_POOL);
        customer.setOwnerId(null);
        customer.setDeptId(null);
        if (!updateById(customer)) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    /**
     * 从公海领取：负责人 / 部门置为当前登录用户，状态恢复正常。
     */
    @Transactional(rollbackFor = Exception.class)
    public void claim(Long id) {
        Customer customer = getById(id);
        if (customer == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "客户不存在");
        }
        if (customer.getStatus() == STATUS_MERGED) {
            throw new BizException("已合并客户不可领取");
        }
        if (customer.getOwnerId() != null) {
            throw new BizException("该客户已被领取");
        }
        var user = com.easykits.crm.common.context.LoginUserContext.require();
        customer.setStatus(STATUS_NORMAL);
        customer.setOwnerId(user.userId());
        customer.setDeptId(user.deptId());
        if (!updateById(customer)) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    /**
     * 公海自动回收：正常状态且超过 N 天无跟进的客户回退公海。
     * 由 CustomerRecycleTask 定时调用（cron：application.yml easy-crm.customer.recycle-cron）。
     */
    @Transactional(rollbackFor = Exception.class)
    public int recycleIdleToPool(int idleDays) {
        LocalDateTime deadline = LocalDateTime.now().minusDays(idleDays);
        List<Customer> idle = list(Wrappers.<Customer>lambdaQuery()
                .eq(Customer::getStatus, STATUS_NORMAL)
                .isNotNull(Customer::getOwnerId)
                .lt(Customer::getLastFollowTime, deadline));
        for (Customer customer : idle) {
            customer.setStatus(STATUS_POOL);
            customer.setOwnerId(null);
            customer.setDeptId(null);
            updateById(customer);
        }
        return idle.size();
    }

    // ============================================================ 私有工具

    /**
     * 组合查重：名称 + 信用代码（信用代码为空时按名称匹配）。excludeId 用于更新场景。
     */
    private Customer findDuplicate(String name, String creditCode, Long excludeId) {
        return getOne(Wrappers.<Customer>lambdaQuery()
                .eq(Customer::getName, name)
                .eq(StringUtils.hasText(creditCode), Customer::getCreditCode, creditCode)
                .isNull(!StringUtils.hasText(creditCode), Customer::getCreditCode)
                .ne(excludeId != null, Customer::getId, excludeId)
                .last("limit 1"), false);
    }

    private void applyForm(Customer customer, String name, String creditCode,
                           Integer industry, Integer scale, Integer region,
                           Integer level, String remark) {
        customer.setName(name);
        customer.setCreditCode(StringUtils.hasText(creditCode) ? creditCode : null);
        customer.setIndustry(industry);
        customer.setScale(scale);
        customer.setRegion(region);
        customer.setLevel(level);
        customer.setRemark(remark);
    }

    private CustomerVO toVO(Customer customer) {
        CustomerVO vo = new CustomerVO();
        vo.setId(customer.getId());
        vo.setName(customer.getName());
        vo.setCreditCode(customer.getCreditCode());
        vo.setIndustry(customer.getIndustry());
        vo.setIndustryName(CustomerDicts.label(CustomerDicts.INDUSTRY, customer.getIndustry()));
        vo.setScale(customer.getScale());
        vo.setScaleName(CustomerDicts.label(CustomerDicts.SCALE, customer.getScale()));
        vo.setRegion(customer.getRegion());
        vo.setRegionName(CustomerDicts.label(CustomerDicts.REGION, customer.getRegion()));
        vo.setLevel(customer.getLevel());
        vo.setLevelName(CustomerDicts.label(CustomerDicts.LEVEL, customer.getLevel()));
        vo.setStatus(customer.getStatus());
        vo.setStatusName(CustomerDicts.STATUS.get(customer.getStatus()));
        vo.setOwnerId(customer.getOwnerId());
        vo.setDeptId(customer.getDeptId());
        vo.setMergedToId(customer.getMergedToId());
        vo.setLastFollowTime(customer.getLastFollowTime());
        vo.setCreateTime(customer.getCreateTime());
        vo.setUpdateTime(customer.getUpdateTime());
        vo.setVersion(customer.getVersion());
        vo.setRemark(customer.getRemark());
        return vo;
    }

    private void fillOwnerDeptNames(List<CustomerVO> vos, List<Customer> customers) {
        List<Long> ownerIds = customers.stream().map(Customer::getOwnerId)
                .filter(Objects::nonNull).distinct().toList();
        List<Long> deptIds = customers.stream().map(Customer::getDeptId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, String> userNameMap = ownerIds.isEmpty() ? Map.of()
                : sysUserMapper.selectBatchIds(ownerIds).stream()
                .collect(Collectors.toMap(u -> u.getId(), u -> u.getNickname()));
        Map<Long, String> deptNameMap = deptIds.isEmpty() ? Map.of()
                : sysDeptMapper.selectBatchIds(deptIds).stream()
                .collect(Collectors.toMap(d -> d.getId(), d -> d.getName()));
        for (CustomerVO vo : vos) {
            vo.setOwnerName(vo.getOwnerId() == null ? null : userNameMap.get(vo.getOwnerId()));
            vo.setDeptName(vo.getDeptId() == null ? null : deptNameMap.get(vo.getDeptId()));
        }
    }
}
