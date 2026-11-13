package cn.easykits.crm.modules.contact.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.ResultCode;
import cn.easykits.crm.common.exception.BizException;
import cn.easykits.crm.entity.Contact;
import cn.easykits.crm.entity.Customer;
import cn.easykits.crm.mapper.ContactMapper;
import cn.easykits.crm.mapper.CustomerMapper;
import cn.easykits.crm.modules.contact.ContactService;
import cn.easykits.crm.modules.contact.dto.ContactCreateForm;
import cn.easykits.crm.modules.contact.dto.ContactInfo;
import cn.easykits.crm.modules.contact.dto.ContactQuery;
import cn.easykits.crm.modules.contact.vo.ContactVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 联系人服务：领域接口（线索转化）+ 管理端 CRUD / 主联系人设置。
 * <p>主联系人唯一性：应用层 + 事务保证（先清后设）——
 * (customer_id, is_primary) 唯一索引会把同客户多条 is_primary=0 的普通联系人判重，故不采用。
 * 可见性随客户（crm_contact 不注册行级数据权限）。
 */
@Service
@RequiredArgsConstructor
public class ContactServiceImpl extends ServiceImpl<ContactMapper, Contact>
        implements ContactService {

    private final CustomerMapper customerMapper;

    // ============================================================ 领域接口（线索转化）

    /**
     * 在客户下创建联系人（转化链路）：客户必须存在。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long customerId, ContactInfo info) {
        if (customerId == null) {
            throw new BizException("创建联系人前必须先选择客户");
        }
        assertCustomerExists(customerId);
        Contact contact = new Contact();
        contact.setCustomerId(customerId);
        contact.setName(info.getName());
        contact.setPhone(info.getPhone());
        contact.setEmail(info.getEmail());
        contact.setPosition(info.getPosition());
        contact.setIsPrimary(0);
        save(contact);
        return contact.getId();
    }

    @Override
    public Long linkExisting(Long contactId) {
        if (contactId == null) {
            throw new BizException("联系人 ID 不能为空");
        }
        Contact contact = getById(contactId);
        if (contact == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "联系人不存在");
        }
        return contactId;
    }

    // ============================================================ 管理端 CRUD

    /**
     * 创建联系人（管理端）：可标记主联系人（标记时同客户其余主联系人自动取消）。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createFromForm(ContactCreateForm form) {
        assertCustomerExists(form.getCustomerId());
        Contact contact = new Contact();
        contact.setCustomerId(form.getCustomerId());
        contact.setName(form.getName());
        contact.setPhone(form.getPhone());
        contact.setEmail(form.getEmail());
        contact.setPosition(form.getPosition());
        contact.setRemark(form.getRemark());
        contact.setIsPrimary(0);
        save(contact);
        if (Boolean.TRUE.equals(form.getIsPrimary())) {
            clearPrimary(contact.getCustomerId(), contact.getId());
            contact.setIsPrimary(1);
            updateById(contact);
        }
        return contact.getId();
    }

    /**
     * 更新联系人（客户归属不可变更，需删除重建）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateFromForm(Long id, ContactCreateForm form) {
        Contact contact = getById(id);
        if (contact == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "联系人不存在");
        }
        contact.setName(form.getName());
        contact.setPhone(form.getPhone());
        contact.setEmail(form.getEmail());
        contact.setPosition(form.getPosition());
        contact.setRemark(form.getRemark());
        if (!updateById(contact)) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
        if (Boolean.TRUE.equals(form.getIsPrimary())) {
            setPrimaryInternal(contact);
        }
    }

    /**
     * 删除联系人（逻辑删除）；主联系人删除后客户暂无主联系人，由业务后续指定。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Contact contact = getById(id);
        if (contact == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "联系人不存在");
        }
        removeById(id);
    }

    /**
     * 按客户查联系人列表（is_primary 倒序 + id 正序，主联系人排最前）。
     */
    public PageResult<ContactVO> pageQuery(ContactQuery query) {
        Page<Contact> page = page(Page.of(query.getPageNum(), query.getPageSize()),
                Wrappers.<Contact>lambdaQuery()
                        .eq(query.getCustomerId() != null, Contact::getCustomerId, query.getCustomerId())
                        .like(StringUtils.hasText(query.getName()), Contact::getName, query.getName())
                        .like(StringUtils.hasText(query.getPhone()), Contact::getPhone, query.getPhone())
                        .orderByDesc(Contact::getIsPrimary)
                        .orderByAsc(Contact::getId));
        List<ContactVO> vos = page.getRecords().stream().map(this::toVO).toList();
        fillCustomerNames(vos, page.getRecords());
        return new PageResult<>(vos, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    /**
     * 设为主联系人（事务内先清后设，保证同一客户主联系人唯一）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void setPrimary(Long id) {
        Contact contact = getById(id);
        if (contact == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "联系人不存在");
        }
        setPrimaryInternal(contact);
    }

    // ============================================================ 私有工具

    private void setPrimaryInternal(Contact contact) {
        // 先清后设：同客户下其他主联系人取消标记
        clearPrimary(contact.getCustomerId(), contact.getId());
        contact.setIsPrimary(1);
        if (!updateById(contact)) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    private void clearPrimary(Long customerId, Long excludeContactId) {
        update(null, Wrappers.<Contact>lambdaUpdate()
                .eq(Contact::getCustomerId, customerId)
                .eq(Contact::getIsPrimary, 1)
                .ne(excludeContactId != null, Contact::getId, excludeContactId)
                .set(Contact::getIsPrimary, 0));
    }

    private void assertCustomerExists(Long customerId) {
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null || customer.getStatus() == 3) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "所属客户不存在或已合并");
        }
    }

    private ContactVO toVO(Contact contact) {
        ContactVO vo = new ContactVO();
        vo.setId(contact.getId());
        vo.setCustomerId(contact.getCustomerId());
        vo.setName(contact.getName());
        vo.setPhone(contact.getPhone());
        vo.setEmail(contact.getEmail());
        vo.setPosition(contact.getPosition());
        vo.setIsPrimary(contact.getIsPrimary() != null && contact.getIsPrimary() == 1);
        vo.setRemark(contact.getRemark());
        vo.setCreateTime(contact.getCreateTime());
        vo.setUpdateTime(contact.getUpdateTime());
        return vo;
    }

    private void fillCustomerNames(List<ContactVO> vos, List<Contact> contacts) {
        List<Long> customerIds = contacts.stream().map(Contact::getCustomerId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, String> nameMap = customerIds.isEmpty() ? Map.of()
                : customerMapper.selectBatchIds(customerIds).stream()
                .collect(Collectors.toMap(Customer::getId, Customer::getName));
        for (ContactVO vo : vos) {
            vo.setCustomerName(vo.getCustomerId() == null ? null : nameMap.get(vo.getCustomerId()));
        }
    }
}
