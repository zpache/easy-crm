package com.easykits.crm.modules.lead.service;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.easykits.crm.common.api.PageResult;
import com.easykits.crm.common.api.ResultCode;
import com.easykits.crm.common.context.LoginUser;
import com.easykits.crm.common.context.LoginUserContext;
import com.easykits.crm.common.exception.BizException;
import com.easykits.crm.entity.Lead;
import com.easykits.crm.entity.SysDept;
import com.easykits.crm.entity.SysUser;
import com.easykits.crm.mapper.LeadMapper;
import com.easykits.crm.mapper.SysDeptMapper;
import com.easykits.crm.mapper.SysUserMapper;
import com.easykits.crm.modules.contact.ContactService;
import com.easykits.crm.modules.contact.dto.ContactInfo;
import com.easykits.crm.modules.customer.CustomerService;
import com.easykits.crm.modules.customer.dto.CustomerInfo;
import com.easykits.crm.modules.lead.constant.LeadSource;
import com.easykits.crm.modules.lead.constant.LeadStatus;
import com.easykits.crm.modules.lead.dto.ConvertForm;
import com.easykits.crm.modules.lead.dto.LeadCreateForm;
import com.easykits.crm.modules.lead.dto.LeadImportRow;
import com.easykits.crm.modules.lead.dto.LeadQuery;
import com.easykits.crm.modules.lead.dto.LeadUpdateForm;
import com.easykits.crm.modules.lead.vo.LeadConvertVO;
import com.easykits.crm.modules.lead.vo.LeadImportResultVO;
import com.easykits.crm.modules.lead.vo.LeadVO;
import com.easykits.crm.modules.opportunity.OpportunityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 线索管理：CRUD、查重、状态机、转化（事务）、公海、Excel 导入、自动回收。
 * <p>列表查询已登记进 {@code DataScopeTableRegistry}（owner_id + dept_id），
 * 行级过滤由数据权限拦截器自动注入。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeadService extends ServiceImpl<LeadMapper, Lead> {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    /** Excel 模板列顺序：线索名称(必填) | 联系人 | 手机号 | 邮箱 | 来源 | 备注，表头校验见 LeadImportListener */

    private final CustomerService customerService;
    private final ContactService contactService;
    private final OpportunityService opportunityService;
    private final SysUserMapper sysUserMapper;
    private final SysDeptMapper sysDeptMapper;

    // ============================================================ 创建 / 更新

    /**
     * 创建线索：负责人取当前登录用户，状态置为新建。
     * 手机号 / 邮箱全局唯一（业务层校验）；公司名重复仅提示不阻断。
     */
    @Transactional(rollbackFor = Exception.class)
    public LeadVO create(LeadCreateForm form) {
        validateContact(form.getPhone(), form.getEmail());
        Lead lead = new Lead();
        applyForm(lead, form.getName(), form.getContactName(), form.getPhone(),
                form.getEmail(), form.getSource(), form.getRemark());
        LoginUser user = LoginUserContext.require();
        lead.setStatus(LeadStatus.NEW.code());
        lead.setOwnerId(user.userId());
        lead.setDeptId(user.deptId());
        lead.setLastFollowTime(LocalDateTime.now());
        save(lead);
        LeadVO vo = toVO(lead, true);
        fillUserAndDeptNames(List.of(vo), List.of(lead));
        return vo;
    }

    /**
     * 更新线索：可编辑基础字段；status 提供时按状态机校验手工流转。
     * 转化后的线索（已验证）不允许再编辑。
     */
    @Transactional(rollbackFor = Exception.class)
    public LeadVO update(Long id, LeadUpdateForm form) {
        Lead lead = getById(id);
        if (lead == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "线索不存在");
        }
        if (LeadStatus.of(lead.getStatus()) == LeadStatus.VERIFIED) {
            throw new BizException("已验证（已转化）的线索不可编辑");
        }
        validateContact(form.getPhone(), form.getEmail());

        String name = StringUtils.hasText(form.getName()) ? form.getName() : lead.getName();
        if (form.getStatus() != null && form.getStatus() != lead.getStatus()) {
            LeadStatus from = LeadStatus.of(lead.getStatus());
            LeadStatus to = LeadStatus.of(form.getStatus());
            if (!from.canManualTransitionTo(to)) {
                throw new BizException("非法状态流转: %s(%d) → %s(%d)，进入「已验证」请走转化接口"
                        .formatted(from.label(), from.code(), to.label(), to.code()));
            }
            lead.setStatus(to.code());
            if (to == LeadStatus.FOLLOWING) {
                lead.setLastFollowTime(LocalDateTime.now());
            }
        }
        applyForm(lead, name, form.getContactName(), form.getPhone(), form.getEmail(),
                form.getSource(), form.getRemark());
        if (!updateById(lead)) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
        LeadVO vo = toVO(lead, true);
        fillUserAndDeptNames(List.of(vo), List.of(lead));
        return vo;
    }

    // ============================================================ 查询

    /**
     * 分页查询（name/phone/status/ownerId/source 过滤）。
     * 行级数据权限条件由拦截器按登录用户自动追加。
     */
    public PageResult<LeadVO> pageQuery(LeadQuery query) {
        Page<Lead> page = page(Page.of(query.getPageNum(), query.getPageSize()),
                Wrappers.<Lead>lambdaQuery()
                        .like(StringUtils.hasText(query.getName()), Lead::getName, query.getName())
                        .like(StringUtils.hasText(query.getPhone()), Lead::getPhone, query.getPhone())
                        .eq(query.getStatus() != null, Lead::getStatus, query.getStatus())
                        // ownerId = -1 表示查公海（owner_id IS NULL）
                        .isNull(query.getOwnerId() != null && query.getOwnerId() == -1, Lead::getOwnerId)
                        .eq(query.getOwnerId() != null && query.getOwnerId() != -1,
                                Lead::getOwnerId, query.getOwnerId())
                        .eq(query.getSource() != null, Lead::getSource, query.getSource())
                        .orderByDesc(Lead::getId));
        List<LeadVO> vos = page.getRecords().stream().map(lead -> toVO(lead, false)).toList();
        fillUserAndDeptNames(vos, page.getRecords());
        return new PageResult<>(vos, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    /**
     * 详情。
     */
    public LeadVO detail(Long id) {
        Lead lead = getById(id);
        if (lead == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "线索不存在");
        }
        LeadVO vo = toVO(lead, false);
        fillUserAndDeptNames(List.of(vo), List.of(lead));
        return vo;
    }

    // ============================================================ 转化

    /**
     * 线索转化（事务）：新建/关联客户、联系人（均可选）+ 必须创建商机，
     * 成功后线索置为已验证并记录三个转化 ID。任一步失败整体回滚。
     * <p>客户 / 联系人 / 商机表轮次 3 建立，当前为占位实现（生成伪 ID、不写库），
     * 接口签名与事务边界不变。
     */
    @Transactional(rollbackFor = Exception.class)
    public LeadConvertVO convert(Long id, ConvertForm form) {
        Lead lead = getById(id);
        if (lead == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "线索不存在");
        }
        LeadStatus status = LeadStatus.of(lead.getStatus());
        if (!status.canConvert()) {
            throw new BizException("当前状态(%s)不可转化".formatted(status.label()));
        }

        // 1. 客户：创建或关联，均可选。真实创建（含名称查重，重复则整笔回滚并提示走关联已有）
        Long customerId = null;
        if (Boolean.TRUE.equals(form.getCreateCustomer())) {
            ConvertForm.CustomerInfo info = form.getCustomerInfo();
            if (info == null || !StringUtils.hasText(info.getName())) {
                throw new BizException("选择创建客户时，客户名称必填");
            }
            CustomerInfo customerInfo = new CustomerInfo();
            customerInfo.setName(info.getName());
            customerInfo.setPhone(info.getPhone());
            customerInfo.setEmail(info.getEmail());
            customerInfo.setAddress(info.getAddress());
            customerId = customerService.create(customerInfo, lead.getOwnerId(), lead.getDeptId());
        } else if (form.getCustomerId() != null) {
            customerId = customerService.linkExisting(form.getCustomerId());
        }

        // 2. 联系人：创建或关联，均可选；联系人必须挂在客户下
        Long contactId = null;
        if (Boolean.TRUE.equals(form.getCreateContact())) {
            ConvertForm.ContactInfo info = form.getContactInfo();
            if (info == null || !StringUtils.hasText(info.getName())) {
                throw new BizException("选择创建联系人时，联系人姓名必填");
            }
            if (customerId == null) {
                throw new BizException("创建联系人前必须先创建或选择客户");
            }
            ContactInfo contactInfo = new ContactInfo();
            contactInfo.setName(info.getName());
            contactInfo.setPhone(info.getPhone());
            contactInfo.setEmail(info.getEmail());
            contactInfo.setPosition(info.getPosition());
            contactId = contactService.create(customerId, contactInfo);
        } else if (form.getContactId() != null) {
            contactId = contactService.linkExisting(form.getContactId());
        }

        // 3. 商机：必须创建
        String opportunityName = StringUtils.hasText(form.getOpportunityName())
                ? form.getOpportunityName() : lead.getName();
        Long opportunityId = opportunityService.create(new OpportunityService.OpportunityInfo(
                opportunityName, lead.getId(), customerId, contactId, lead.getOwnerId(), lead.getDeptId()));

        // 4. 回写线索：状态置为已验证 + 记录转化 ID
        lead.setStatus(LeadStatus.VERIFIED.code());
        lead.setConvertedCustomerId(customerId);
        lead.setConvertedContactId(contactId);
        lead.setConvertedOpportunityId(opportunityId);
        lead.setLastFollowTime(LocalDateTime.now());
        if (!updateById(lead)) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
        log.info("线索转化成功: leadId={}, customerId={}, contactId={}, opportunityId={}",
                lead.getId(), customerId, contactId, opportunityId);
        return new LeadConvertVO(customerId, contactId, opportunityId);
    }

    // ============================================================ 公海

    /**
     * 放入公海：负责人置空、状态回退为新建。仅新建 / 跟进中的线索可放入。
     */
    @Transactional(rollbackFor = Exception.class)
    public void toPool(Long id) {
        Lead lead = getById(id);
        if (lead == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "线索不存在");
        }
        if (lead.getOwnerId() == null) {
            throw new BizException("该线索已在公海");
        }
        LeadStatus status = LeadStatus.of(lead.getStatus());
        if (status != LeadStatus.NEW && status != LeadStatus.FOLLOWING) {
            throw new BizException("当前状态(%s)不可放入公海".formatted(status.label()));
        }
        lead.setOwnerId(null);
        lead.setDeptId(null);
        lead.setStatus(LeadStatus.NEW.code());
        if (!updateById(lead)) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    /**
     * 从公海领取：负责人 / 部门置为当前登录用户，状态保持新建。
     */
    @Transactional(rollbackFor = Exception.class)
    public void claim(Long id) {
        Lead lead = getById(id);
        if (lead == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "线索不存在");
        }
        if (lead.getOwnerId() != null) {
            throw new BizException("该线索已被领取");
        }
        LoginUser user = LoginUserContext.require();
        lead.setOwnerId(user.userId());
        lead.setDeptId(user.deptId());
        if (!updateById(lead)) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    /**
     * 公海自动回收：跟进中且超过 N 天无跟进记录的线索回退公海。
     * 由 {@code LeadRecycleTask} 定时调用（cron 配置：application.yml 的 easy-crm.lead.recycle-cron）。
     *
     * @return 回收条数
     */
    @Transactional(rollbackFor = Exception.class)
    public int recycleIdleToPool(int idleDays) {
        LocalDateTime deadline = LocalDateTime.now().minusDays(idleDays);
        List<Lead> idle = list(Wrappers.<Lead>lambdaQuery()
                .eq(Lead::getStatus, LeadStatus.FOLLOWING.code())
                .isNotNull(Lead::getOwnerId)
                .lt(Lead::getLastFollowTime, deadline));
        for (Lead lead : idle) {
            lead.setOwnerId(null);
            lead.setDeptId(null);
            lead.setStatus(LeadStatus.NEW.code());
            updateById(lead);
        }
        return idle.size();
    }

    // ============================================================ Excel 导入

    /**
     * Excel 批量导入（EasyExcel 流式解析）：逐行校验、错误行集中返回，单行失败不影响其余行。
     * 模板列：线索名称 | 联系人 | 手机号 | 邮箱 | 来源 | 备注。
     * 文件内校验（必填 / 格式 / 文件内重复）在监听器完成；库内查重在解析后统一执行。
     * 导入的线索负责人 / 部门为当前登录用户，状态为新建。
     */
    @Transactional(rollbackFor = Exception.class)
    public LeadImportResultVO importExcel(MultipartFile file) {
        LoginUser user = LoginUserContext.require();
        LeadImportListener listener = new LeadImportListener();
        try {
            EasyExcel.read(file.getInputStream(), LeadImportRow.class, listener)
                    .sheet()
                    .headRowNumber(1)
                    .doRead();
        } catch (Exception e) {
            if (listener.isHeaderInvalid()) {
                throw new BizException("模板不正确：第 1 列表头应为「%s」"
                        .formatted(LeadImportListener.REQUIRED_HEADER));
            }
            log.error("Excel 解析失败", e);
            throw new BizException("文件解析失败，请使用标准模板的 .xlsx 文件");
        }

        // 库内查重 + 组装入库（错误行继续收集，不中断）
        List<LeadImportResultVO.RowError> errors = new ArrayList<>(listener.getErrors());
        List<Lead> toSave = new ArrayList<>();
        for (LeadImportListener.ParsedRow parsed : listener.getRows()) {
            LeadImportRow row = parsed.row();
            List<String> rowErrors = new ArrayList<>();
            String phone = StringUtils.hasText(row.getPhone()) ? row.getPhone().trim() : null;
            String email = StringUtils.hasText(row.getEmail()) ? row.getEmail().trim() : null;
            if (phone != null) {
                if (!PHONE_PATTERN.matcher(phone).matches()) {
                    rowErrors.add("手机号格式不正确: " + phone);
                } else if (exists(Wrappers.<Lead>lambdaQuery().eq(Lead::getPhone, phone))) {
                    rowErrors.add("手机号已存在: " + phone);
                }
            }
            if (email != null && exists(Wrappers.<Lead>lambdaQuery().eq(Lead::getEmail, email))) {
                rowErrors.add("邮箱已存在: " + email);
            }
            if (!rowErrors.isEmpty()) {
                errors.add(new LeadImportResultVO.RowError(parsed.rowIndex(), String.join("; ", rowErrors)));
                continue;
            }

            Lead lead = new Lead();
            lead.setName(row.getName().trim());
            lead.setContactName(StringUtils.hasText(row.getContactName()) ? row.getContactName().trim() : null);
            lead.setPhone(phone);
            lead.setEmail(email);
            lead.setSource(LeadSource.codeOfLabel(row.getSource()));
            lead.setStatus(LeadStatus.NEW.code());
            lead.setOwnerId(user.userId());
            lead.setDeptId(user.deptId());
            lead.setRemark(StringUtils.hasText(row.getRemark()) ? row.getRemark().trim() : null);
            lead.setLastFollowTime(LocalDateTime.now());
            toSave.add(lead);
        }

        toSave.forEach(this::save);
        return new LeadImportResultVO(listener.getTotal(), toSave.size(), errors.size(), errors);
    }

    // ============================================================ 私有工具

    private void applyForm(Lead lead, String name, String contactName,
                           String phone, String email, Integer source, String remark) {
        lead.setName(name);
        lead.setContactName(contactName);
        lead.setPhone(StringUtils.hasText(phone) ? phone : null);
        lead.setEmail(StringUtils.hasText(email) ? email : null);
        if (source != null && !LeadSource.isValid(source)) {
            throw new BizException("来源字典值不合法: " + source);
        }
        lead.setSource(source);
        lead.setRemark(remark);
    }

    /**
     * 手机号 / 邮箱全局唯一校验（含手机号格式校验）。
     */
    private void validateContact(String phone, String email) {
        if (StringUtils.hasText(phone)) {
            if (!PHONE_PATTERN.matcher(phone).matches()) {
                throw new BizException("手机号格式不正确: " + phone);
            }
            if (exists(Wrappers.<Lead>lambdaQuery().eq(Lead::getPhone, phone))) {
                throw new BizException("手机号已存在: " + phone);
            }
        }
        if (StringUtils.hasText(email)) {
            if (exists(Wrappers.<Lead>lambdaQuery().eq(Lead::getEmail, email))) {
                throw new BizException("邮箱已存在: " + email);
            }
        }
    }

    /**
     * 实体转 VO；withDuplicate=true 时附带同名线索提示（不做阻断）。
     */
    private LeadVO toVO(Lead lead, boolean withDuplicate) {
        LeadVO vo = new LeadVO();
        vo.setId(lead.getId());
        vo.setName(lead.getName());
        vo.setContactName(lead.getContactName());
        vo.setPhone(lead.getPhone());
        vo.setEmail(lead.getEmail());
        vo.setSource(lead.getSource());
        vo.setSourceName(LeadSource.labelOf(lead.getSource()));
        vo.setStatus(lead.getStatus());
        vo.setStatusName(LeadStatus.of(lead.getStatus()).label());
        vo.setOwnerId(lead.getOwnerId());
        vo.setDeptId(lead.getDeptId());
        vo.setRemark(lead.getRemark());
        vo.setConvertedCustomerId(lead.getConvertedCustomerId());
        vo.setConvertedContactId(lead.getConvertedContactId());
        vo.setConvertedOpportunityId(lead.getConvertedOpportunityId());
        vo.setLastFollowTime(lead.getLastFollowTime());
        vo.setCreateTime(lead.getCreateTime());
        vo.setUpdateTime(lead.getUpdateTime());
        vo.setVersion(lead.getVersion());

        if (withDuplicate) {
            List<Lead> sameName = list(Wrappers.<Lead>lambdaQuery()
                    .eq(Lead::getName, lead.getName())
                    .ne(lead.getId() != null, Lead::getId, lead.getId())
                    .last("limit 5"));
            vo.setCompanyDuplicate(!sameName.isEmpty());
            vo.setDuplicates(sameName.stream()
                    .map(d -> new LeadVO.DuplicateItem(d.getId(), d.getName()))
                    .toList());
        }
        return vo;
    }

    /**
     * 批量补负责人姓名与部门名（一次查询，避免逐行查库）。
     */
    private void fillUserAndDeptNames(List<LeadVO> vos, List<Lead> leads) {
        List<Long> ownerIds = leads.stream().map(Lead::getOwnerId).filter(Objects::nonNull).distinct().toList();
        List<Long> deptIds = leads.stream().map(Lead::getDeptId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> userNameMap = ownerIds.isEmpty() ? Map.of()
                : sysUserMapper.selectBatchIds(ownerIds).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getNickname));
        Map<Long, String> deptNameMap = deptIds.isEmpty() ? Map.of()
                : sysDeptMapper.selectBatchIds(deptIds).stream()
                .collect(Collectors.toMap(SysDept::getId, SysDept::getName));
        for (LeadVO vo : vos) {
            vo.setOwnerName(vo.getOwnerId() == null ? null : userNameMap.get(vo.getOwnerId()));
            vo.setDeptName(vo.getDeptId() == null ? null : deptNameMap.get(vo.getDeptId()));
        }
    }
}
