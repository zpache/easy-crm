package com.easykits.crm.modules.contact;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 联系人服务占位实现（联系人表轮次 3 建立）。
 */
@Slf4j
@Service
public class ContactServicePlaceholder implements ContactService {

    @Override
    public Long create(ContactInfo info) {
        // TODO（轮次 3）：INSERT crm_contact 并返回自增主键
        Long placeholderId = System.currentTimeMillis();
        log.info("[占位] 创建联系人（未落库）: name={}, placeholderId={}", info.name(), placeholderId);
        return placeholderId;
    }

    @Override
    public Long linkExisting(Long contactId) {
        if (contactId == null) {
            throw new IllegalArgumentException("联系人 ID 不能为空");
        }
        // TODO（轮次 3）：SELECT crm_contact 校验存在性
        log.info("[占位] 关联联系人（未校验库）: contactId={}", contactId);
        return contactId;
    }
}
