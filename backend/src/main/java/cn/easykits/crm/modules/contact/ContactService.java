package cn.easykits.crm.modules.contact;

import cn.easykits.crm.modules.contact.dto.ContactInfo;

/**
 * 联系人领域服务接口（跨模块调用入口，线索转化等使用）。
 * 真实实现 {@link ContactServiceImpl}：联系人必须挂在客户下。
 */
public interface ContactService {

    /**
     * 在指定客户下创建联系人，返回联系人 ID。
     */
    Long create(Long customerId, ContactInfo info);

    /**
     * 关联已有联系人：校验存在性、且必须属于指定客户（防止跨客户挂载），
     * 返回联系人 ID，不存在抛 DATA_NOT_FOUND。
     */
    Long linkExisting(Long contactId, Long customerId);

    /**
     * 客户合并时的联系人迁移：把 source 客户下的联系人全部迁到 target 客户下。
     * 由 CustomerService.merge 在同一事务内调用（跨模块表写收敛到本服务）。
     */
    void migrateToCustomer(Long sourceCustomerId, Long targetCustomerId);
}
