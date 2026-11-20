package cn.easykits.crm.modules.order.listener;

import cn.easykits.crm.common.datascope.DataScopeContext;
import cn.easykits.crm.modules.opportunity.event.OpportunityWonEvent;
import cn.easykits.crm.modules.order.service.OrderServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 商机赢单联动组件（批次 3 新增，不改动现有 OpportunityEventListener / 事件类）。
 * <p>与现有监听器并存：现有监听器仍只打日志，本组件负责真正建单。
 * <p>统一在 AFTER_COMMIT + @Async 消费，避免事务未提交就被下游读取；
 * 异步线程无登录态，OrderServiceImpl.createDraftFromWon 完全不依赖 LoginUserContext。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpportunityWonOrderListener {

    private final OrderServiceImpl orderService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOpportunityWon(OpportunityWonEvent event) {
        try {
            // 异步线程无登录态，而行级数据权限 fail closed：
            // 必须以系统身份执行，否则 createDraftFromWon 内查询商机主数据被拒绝，deptId 会丢失
            Long orderId = DataScopeContext.callAsSystem(
                    () -> orderService.createDraftFromWon(event));
            log.info("[赢单联动] opportunityId={}, name={}, 自动创建订单草稿 id={} 及首期回款计划",
                    event.opportunityId(), event.name(), orderId);
        } catch (Exception e) {
            // 异步线程内异常不会冒泡到主事务，必须显式记录，便于补偿排查
            log.error("[赢单联动] 自动建单失败 opportunityId={}, name={}",
                    event.opportunityId(), event.name(), e);
        }
    }
}
