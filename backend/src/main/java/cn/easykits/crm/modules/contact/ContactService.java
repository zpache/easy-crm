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
     * 关联已有联系人：校验存在性并返回联系人 ID，不存在抛 DATA_NOT_FOUND。
     */
    Long linkExisting(Long contactId);
}
