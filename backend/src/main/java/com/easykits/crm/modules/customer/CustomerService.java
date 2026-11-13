package com.easykits.crm.modules.customer;

/**
 * 客户服务接口。
 * <p><b>占位说明（轮次 3 实现）</b>：客户表（crm_customer）本轮未建，实现类为空壳：
 * 创建只生成占位 ID、不写库；关联只做空值校验。轮次 3 建表后替换实现，
 * 接口签名保持不变，线索转化的事务边界不受影响。
 */
public interface CustomerService {

    /**
     * 创建客户，返回客户 ID。
     */
    Long create(CustomerInfo info);

    /**
     * 关联已有客户：校验存在性并返回客户 ID。
     */
    Long linkExisting(Long customerId);

    /**
     * 新建客户信息（轮次 3 落库时按此建 DTO/校验）。
     */
    record CustomerInfo(String name, String phone, String email, String address) {
    }
}
