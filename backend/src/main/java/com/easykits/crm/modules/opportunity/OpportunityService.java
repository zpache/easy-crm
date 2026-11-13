package com.easykits.crm.modules.opportunity;

/**
 * 商机服务接口。
 * <p><b>占位说明（轮次 3 实现）</b>：商机表（crm_opportunity）本轮未建，实现类为空壳；
 * 线索转化接口要求「必须同时创建商机」，占位实现只生成占位 ID，不写库。
 * 接口签名保持不变，轮次 3 建表后替换实现，转化的事务边界不受影响。
 */
public interface OpportunityService {

    /**
     * 由线索转化创建商机，返回商机 ID。
     */
    Long create(OpportunityInfo info);

    /**
     * 商机创建参数。
     *
     * @param name       商机名称（默认取线索名称）
     * @param leadId     来源线索 ID
     * @param customerId 关联客户 ID（可为空，轮次 3 决定是否允许）
     * @param contactId  关联联系人 ID（可为空）
     * @param ownerId    负责人（继承线索负责人）
     * @param deptId     归属部门（继承线索部门）
     */
    record OpportunityInfo(String name, Long leadId, Long customerId,
                           Long contactId, Long ownerId, Long deptId) {
    }
}
