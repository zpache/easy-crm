package com.easykits.crm.modules.lead.vo;

/**
 * 线索转化结果：返回转化生成的客户 / 联系人 / 商机 ID。
 * 占位服务阶段 ID 为毫秒时间戳伪 ID，轮次 3 建表后为真实自增主键。
 */
public record LeadConvertVO(Long customerId, Long contactId, Long opportunityId) {
}
