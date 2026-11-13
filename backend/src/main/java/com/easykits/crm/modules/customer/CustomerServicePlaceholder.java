package com.easykits.crm.modules.customer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 客户服务占位实现（客户表轮次 3 建立）。
 * 占位 ID 用毫秒时间戳生成，仅用于线索转化的字段回填演示；轮次 3 替换为真实自增主键。
 */
@Slf4j
@Service
public class CustomerServicePlaceholder implements CustomerService {

    @Override
    public Long create(CustomerInfo info) {
        // TODO（轮次 3）：INSERT crm_customer 并返回自增主键
        Long placeholderId = System.currentTimeMillis();
        log.info("[占位] 创建客户（未落库）: name={}, placeholderId={}", info.name(), placeholderId);
        return placeholderId;
    }

    @Override
    public Long linkExisting(Long customerId) {
        if (customerId == null) {
            throw new IllegalArgumentException("客户 ID 不能为空");
        }
        // TODO（轮次 3）：SELECT crm_customer 校验存在性，不存在抛 DATA_NOT_FOUND
        log.info("[占位] 关联客户（未校验库）: customerId={}", customerId);
        return customerId;
    }
}
