package cn.easykits.crm.modules.opportunity.event;

/**
 * 商机赢单事件。事务提交后（AFTER_COMMIT）发布；
 * 轮次 5 监听器接订单模块（自动创建订单/回款计划），当前仅打日志。
 *
 * @param opportunityId   商机 ID
 * @param name            商机名称
 * @param customerId      客户 ID
 * @param ownerId         负责人
 * @param actualAmount    实际成交金额
 * @param actualCloseDate 实际成交日期
 */
public record OpportunityWonEvent(Long opportunityId, String name, Long customerId,
                                  Long ownerId, java.math.BigDecimal actualAmount,
                                  java.time.LocalDate actualCloseDate) {
}
