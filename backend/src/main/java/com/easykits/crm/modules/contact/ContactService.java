package com.easykits.crm.modules.contact;

/**
 * 联系人服务接口。
 * <p><b>占位说明（轮次 3 实现）</b>：联系人表（crm_contact）本轮未建，实现类为空壳；
 * 接口签名保持不变，线索转化的事务边界不受影响。
 */
public interface ContactService {

    /**
     * 创建联系人，返回联系人 ID。
     */
    Long create(ContactInfo info);

    /**
     * 关联已有联系人：校验存在性并返回联系人 ID。
     */
    Long linkExisting(Long contactId);

    /**
     * 新建联系人信息。
     */
    record ContactInfo(String name, String phone, String email, String position) {
    }
}
