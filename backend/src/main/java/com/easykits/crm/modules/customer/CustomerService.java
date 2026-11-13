package com.easykits.crm.modules.customer;

import com.easykits.crm.modules.customer.dto.CustomerInfo;

/**
 * 客户领域服务接口（跨模块调用入口，线索转化等使用）。
 * <p>真实实现 {@link CustomerServiceImpl}：创建时做「名称 + 信用代码」组合查重，
 * 重复抛出业务异常并携带已存在客户 ID。
 */
public interface CustomerService {

    /**
     * 创建客户，返回客户 ID。名称 + 信用代码组合查重，重复抛 BizException（含已存在 ID）。
     */
    Long create(CustomerInfo info, Long ownerId, Long deptId);

    /**
     * 关联已有客户：校验存在性并返回客户 ID，不存在抛 DATA_NOT_FOUND。
     */
    Long linkExisting(Long customerId);
}
