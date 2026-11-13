package cn.easykits.crm.modules.opportunity.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 商机事件监听器：统一在事务提交后（AFTER_COMMIT）触发，避免事务未提交就被下游消费。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpportunityEventListener {

    /**
     * 赢单事件：TODO（轮次 5 订单模块上线后）自动创建订单与回款计划；当前仅打日志。
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOpportunityWon(OpportunityWonEvent event) {
        log.info("[商机赢单] opportunityId={}, name={}, customerId={}, actualAmount={}, actualCloseDate={}",
                event.opportunityId(), event.name(), event.customerId(),
                event.actualAmount(), event.actualCloseDate());
        // TODO（轮次 5）：调用订单服务创建订单 + 首期回款计划
    }

    /**
     * 阶段变更事件：TODO（轮次 4 后续）写商机动态/跟进时间线；当前仅打日志。
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStageChanged(OpportunityStageChangedEvent event) {
        log.info("[商机阶段变更] opportunityId={}, name={}, {} -> {}",
                event.opportunityId(), event.name(),
                event.fromStageCode(), event.toStageCode());
    }
}
