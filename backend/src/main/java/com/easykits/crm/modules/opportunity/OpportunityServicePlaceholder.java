package com.easykits.crm.modules.opportunity;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 商机服务占位实现（商机表轮次 3 建立）。
 */
@Slf4j
@Service
public class OpportunityServicePlaceholder implements OpportunityService {

    @Override
    public Long create(OpportunityInfo info) {
        // TODO（轮次 3）：INSERT crm_opportunity（含来源线索冗余字段）并返回自增主键
        Long placeholderId = System.currentTimeMillis();
        log.info("[占位] 创建商机（未落库）: name={}, leadId={}, placeholderId={}",
                info.name(), info.leadId(), placeholderId);
        return placeholderId;
    }
}
