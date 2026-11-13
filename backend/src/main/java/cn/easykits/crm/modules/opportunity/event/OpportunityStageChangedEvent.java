package cn.easykits.crm.modules.opportunity.event;

/**
 * 商机阶段变更事件。事务提交后（AFTER_COMMIT）发布，监听器打日志。
 *
 * @param opportunityId 商机 ID
 * @param name          商机名称
 * @param fromStageCode 原阶段编码
 * @param toStageCode   新阶段编码
 * @param ownerId       负责人
 */
public record OpportunityStageChangedEvent(Long opportunityId, String name,
                                           String fromStageCode, String toStageCode,
                                           Long ownerId) {
}
